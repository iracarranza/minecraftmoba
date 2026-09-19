// Protocol-level navigation probe for a materialized terrain gallery.
// Asserts dimension, position and game mode after each function call.
// This exercises the datapack functions over the real protocol. It is NOT a
// human client walkthrough and proves nothing about how the terrain looks.
const fs = require('node:fs');
const mineflayer = require('mineflayer');

const out = process.env.HARVEST_RESULT;
const manifest = JSON.parse(fs.readFileSync(process.env.HARVEST_MANIFEST, 'utf8'));
const ids = manifest.volumes.map(v => v.volume_id);
const targets = manifest.targets;
const checks = [];
const record = (name, ok, detail) => { checks.push({ name, ok, ...detail }); console.log((ok ? 'OK  ' : 'FAIL') + ' ' + name + ' ' + JSON.stringify(detail)); };

const bot = mineflayer.createBot({
  host: '127.0.0.1', port: Number(process.env.HARVEST_PORT),
  username: process.env.HARVEST_PLAYER, auth: 'offline', version: '1.21.11',
});

const chat = [];
bot.on('messagestr', m => chat.push(m));
bot.on('error', e => { console.log('ERROR ' + e.message); finish(1); });
bot.on('kicked', r => { console.log('KICKED ' + JSON.stringify(r)); finish(1); });

const sleep = ms => new Promise(r => setTimeout(r, ms));
async function run(command) { bot.chat('/' + command); await sleep(600); }

// bot.game.dimension reports the dimension TYPE, which is harvest:inspection for
// every dimension in this gallery, so it cannot distinguish volumes. Ask the
// server for the player's actual dimension instead.
async function dimension() {
  const before = chat.length;
  bot.chat('/data get entity ' + bot.username + ' Dimension');
  await sleep(600);
  const line = chat.slice(before).find(m => m.includes('entity data'));
  const m = line && line.match(/"([^"]+)"/);
  if (!m) throw new Error('could not read dimension: ' + (line || 'no response'));
  return m[1];
}

async function state() {
  const p = bot.entity.position;
  return { dimension: await dimension(), dimension_type: bot.game.dimension,
           mode: bot.game.gameMode, pos: [p.x, p.y, p.z] };
}

// Ticks are frozen, so an exact teleport target should not drift at all.
async function expect(name, want, tolerance = 0.51) {
  const s = await state();
  const posOk = want.pos === undefined || want.pos.every((v, i) => Math.abs(s.pos[i] - v) <= tolerance);
  const dimOk = want.dimension === undefined || s.dimension === want.dimension;
  const modeOk = want.mode === undefined || s.mode === want.mode;
  record(name, posOk && dimOk && modeOk, { want, got: s });
}

function finish(code) {
  fs.writeFileSync(out, JSON.stringify({
    schema: 'terrain_gallery_navigation/1',
    scope: 'protocol-level function navigation against a disposable server; no human client inspection',
    checks, chat_lines: chat.length,
    pass: code === 0 && checks.length > 0 && checks.every(c => c.ok),
  }, null, 2) + '\n');
  try { bot.quit(); } catch (e) {}
  process.exit(code);
}

bot.once('spawn', async () => {
  try {
    await sleep(1500);
    await run('gamemode adventure @s');

    await run('function harvest:hub');
    await expect('hub', { dimension: 'minecraft:overworld', mode: 'adventure', pos: [0.5, 65, 0.5] });

    // next from the hub walks 0,1,2 then wraps to 0.
    for (let i = 0; i < ids.length + 1; i++) {
      const id = ids[i % ids.length];
      await run('function harvest:next');
      await expect('next->' + (i % ids.length) + (i >= ids.length ? ' (wrapped)' : ''),
        { dimension: 'harvest:' + id, mode: 'adventure', pos: targets[id].map((v, k) => k === 1 ? v : v + 0.5) });
    }

    // previous from volume 0 must wrap backwards to the last volume.
    await run('function harvest:previous');
    const last = ids[ids.length - 1];
    await expect('previous wraps to last', { dimension: 'harvest:' + last, pos: targets[last].map((v, k) => k === 1 ? v : v + 0.5) });

    for (const id of ids) {
      await run('function harvest:visit/' + id);
      await expect('visit ' + id, { dimension: 'harvest:' + id, mode: 'adventure', pos: targets[id].map((v, k) => k === 1 ? v : v + 0.5) });

      await run('function harvest:overview/' + id);
      await expect('overview ' + id + ' is spectator', { dimension: 'harvest:' + id, mode: 'spectator' });

      // Round trip: visit must restore adventure inspection after an overview.
      await run('function harvest:visit/' + id);
      await expect('visit restores adventure after overview ' + id,
        { dimension: 'harvest:' + id, mode: 'adventure', pos: targets[id].map((v, k) => k === 1 ? v : v + 0.5) });
    }

    record('dimension type is shared across volumes', bot.game.dimension === 'harvest:inspection',
      { dimension_type: bot.game.dimension });

    const before = chat.length;
    await run('function harvest:index');
    await sleep(600);
    const printed = chat.slice(before).join('\n');
    // The index now shows a compact clickable label; the volume id lives in the
    // hover text, which a bot cannot read, so assert the label instead.
    for (let i = 0; i < ids.length; i++) {
      const label = '[' + (i + 1) + ']';
      record('index lists entry ' + label, printed.includes(label), { found: printed.includes(label) });
    }
    record('index is compact', printed.split('\n').filter(l => l.trim()).length <= ids.length + 2,
      { lines: printed.split('\n').filter(l => l.trim()).length, volumes: ids.length });

    finish(0);
  } catch (e) {
    console.log('EXCEPTION ' + e.stack);
    finish(1);
  }
});

setTimeout(() => { console.log('TIMEOUT'); finish(1); }, 240000);
