. yes agree and continue













Worked for 59s

Analyzed
Analyzed
The inventory-inclusive pass strengthens the same conclusion, with one correction: time diversion is the dominant penalty first; inventory becomes a sharp secondary penalty specifically at very low working capacity or high item diversity.
Everything below is still a sensitivity fixture.
Inventory-inclusive C-region pass
I kept the same six-minute C-region structure but added:
- ambient coal, iron, copper, gravel, clay, redstone;
- minor pockets of the same;
- one major copper manifestation;
- real stack consolidation;
- finite working inventory after loadout/food;
- a rule that the player cannot collect an incidental pickup if doing so would exceed working cargo space.
The major copper manifestation yields 96 copper, so it needs roughly two stacks by itself.
I tested four working-capacity cases:
4 working slots
5 working slots
7 working slots
10 working slots

“Working slots” means after equipment, food, utilities, etc.
1. The 7-working-slot case
This is a useful mid-early benchmark.
Policy	Incidental diversion	Avg incidental material	Major copper completed
Vacuum	29.0%	~46 units	16%
Selective	12.8%	~18 units	77%
Destination	3.3%	~5 units	99.5%


The result is pretty severe.
The vacuum player reaches the copper opportunity around:
5:29

on average.
The selective player reaches it around:
4:31

The destination-focused player reaches it around:
3:57

Since the copper operation itself takes about 70 seconds in this fixture, that explains most of the completion difference.
So at this capacity, the main punishment for vacuuming is still:
you spent the expedition doing something else.

Not inventory.
And I think that's desirable.
2. Inventory becomes decisive below that
At 4 working slots:
Policy	Forced cargo failure	Major copper completed
Vacuum	43%	5.8%
Selective	0.9%	69.6%
Destination	~0%	99.3%


Now the vacuum player hits both penalties:
too much diversion
+
too much item variety
A small collection like:
coal
iron
copper
gravel
clay
redstone
doesn't occupy much quantity, but it can occupy six different stacks.
That matters enormously when the player only has four usable cargo slots.
This is exactly the kind of pressure the early constrained-character model was supposed to create.
3. Five working slots is a transition point
At 5 working slots:
Policy	Forced cargo failure	Major copper completed
Vacuum	9.6%	13.3%
Selective	~0%	76.9%
Destination	~0%	99.5%


The interesting thing is that inventory failure drops dramatically from 43% to ~10% with only one extra working slot.
That tells us something important:
Early inventory growth may produce large qualitative changes in expedition behavior even when the nominal slot increase looks small.

Going from four to five usable cargo slots isn't merely “+25% inventory.”
It can mean:
before:
I cannot safely carry incidental diversity and still reserve space for destination cargo

after:
I can take one or two extra material categories without compromising the mission
That's a meaningful progression breakpoint.
4. Above ~7 working slots, inventory pressure largely stops being the main differentiator
At 7 and 10 working slots, the vacuum policy almost never fails because of slot count.
But it still performs terribly because it burns too much time.
That gives us a nice layered progression:
Very early
Inventory is a hard strategic constraint.
Early-mid
Inventory still shapes what categories you can carry,
but time/opportunity cost becomes more important.
Later
Inventory is less restrictive,
so judgment increasingly shifts toward travel,
time, strategic demand, and opportunity significance.

That fits the broader progression philosophy surprisingly well.
5. This creates a real purpose for early inventory progression
Inventory growth is not merely:
more loot
It's:
more simultaneous economic relationships
At low capacity, the player has to choose:
coal?
iron?
copper?
food?
construction material?
specialty resource?
destination payload?
As capacity grows, the player can participate in more of those economies simultaneously.
That means inventory progression naturally changes playstyle from:
MISSION-SPECIFIC
toward:
FLEXIBLE MINECRAFT GENERALIST
which is exactly what we've said the early-to-mid progression curve should do.
6. It also supports the “constrained Minecraft player → complete Minecraft player” model
This is probably the strongest systemic connection so far.
At the beginning, a six-slot character isn't simply weaker.
They are structurally forced into behavior like:
"I am going out for copper."

not

"I am going to wander around and bring home everything useful."

Later, with vanilla-scale inventory:
"I can go out for copper and also bring back
coal, iron, food, mob drops, and whatever else I find."

So the progression isn't artificial combat scaling.
It is literally restoring ordinary Minecraft flexibility.
That's extremely clean.
7. Background vanilla frequency should therefore remain meaningful
This answers your earlier question more firmly.
I would not suppress ordinary resource incidence just because strategic manifestations exist.
The player should regularly see things like:
2 coal
1 exposed iron
3 copper
a gravel pocket
a rabbit
some berries

because those things create the economic filtering problem.
If we remove them, inventory barely matters.
Then every expedition becomes:
travel to node
harvest node
return
which is much less Minecraft.
The ambient layer is doing important game-design work.
8. But the ambient field must remain geographically coherent
We shouldn't independently roll:
coal
iron
copper
redstone
clay
at arbitrary global rates.
The better structure remains:
Opportunity Relationship
→ establishes resource vocabulary and incidence
→ ambient signals
→ minor pockets
→ strategic manifestations
Example:
Highland mineral relationship
Could support:
coal: common ambient
iron: moderate ambient
copper: moderate/elevated ambient
gravel: occasional
clay: near-zero
redstone: low unless deeper
River/wetland relationship
Could support:
clay: common ambient/pockets
sand: common
gravel: common
iron: occasional geological exposure
copper: low
This keeps the world legible.
The player can learn:
“If I want clay, this is the kind of geography I should search.”

not:
“Clay nodes randomly spawn anywhere.”

9. The ambient field can also act as a confidence signal
This is where Extraction gets substantially richer.
Suppose the Extractor enters a highland region and sees:
1 copper block
→ 2 more exposed copper
→ small copper pocket
The rational inference becomes:
P(meaningful copper opportunity nearby) ↑
The player may then invest more time in prospecting.
But if they instead see:
coal
coal
iron
coal
they may conclude:
this geology is productive,
but perhaps not especially copper-rich
So the resource field doesn't merely create material decisions.
It creates information.
That gives ordinary ore a second value:
MATERIAL VALUE
+
SEARCH INFORMATION VALUE
10. We can now distinguish three kinds of incidental pickup
This probably belongs in the formal terminology.
Convenience Pickup
Almost zero diversion.
ore is literally on the path

Typical rational behavior:
usually take if useful

Opportunity Detour
Small but noticeable diversion.
visible vein 8 blocks off path
small cave branch

Decision depends on state.
Secondary Operation
Enough commitment that pursuing it meaningfully competes with the original mission.
minor iron vein deeper down branch
large clay bed
separate coal pocket

This is no longer “something you picked up on the way.”
It is another task.
That distinction matters more than raw quantity.
11. A better diversion model
Rather than categorizing strictly by blocks, use:
DIVERSION COST
=
path deviation
+ exploitation time
+ inventory footprint
+ information uncertainty
+ return consequences

A 10-ore vein directly intersecting the route may be a tiny diversion.
A 3-ore vein behind a dangerous cave turn may be substantial.
So:
payload ≠ commitment

Again this points us back to worldgen Commitment Profile instead of a pure block-count model.
12. The rigorous simulation suggests a useful progression interaction
Because inventory expands with level, the same world opportunity naturally changes value over the match.
Example:
Lv1-ish character
working capacity: 3–4 slots

2 coal:
"maybe not worth opening a coal stack"

iron:
"probably worth one stack"

copper destination:
"reserve two slots"

Lv8-ish character
working capacity: 7–10 slots

2 coal:
"cheap enough"

iron:
"take it"

copper destination:
still plenty of space

Vanilla-scale character
inventory rarely forces rejection
time and strategic priorities dominate

So resource relevance itself evolves through character progression without changing the resource.
That is a major benefit of the inventory system.
13. This also means we have to be careful with automatic inventory growth
The current progression curve has inventory growing fairly aggressively.
That's good if the intended progression is:
very constrained
→ rapidly competent
→ vanilla-complete around midgame

But this simulation shows that every few slots can remove a large amount of economic friction.
Especially:
4 working → 5 working

was a very large behavioral shift.
So when we return to the XP/capacity curve, we shouldn't evaluate inventory milestones merely as:
slot count by level

We should evaluate:
HOW MANY RESOURCE CATEGORIES
CAN THE PLAYER PRACTICALLY SUPPORT
WHILE PRESERVING MISSION CARGO?

That's a much better metric.
Call it something like:
Economic Breadth Capacity
Economic Breadth Capacity
=
number of materially distinct resource stacks
that can be carried without compromising
provisioning + mission payload

This may be more meaningful than raw inventory slots.
14. Example
Suppose:
12 total slots
3 equipment
2 food/utilities
2 reserved copper payload

Then:
Economic Breadth Capacity
= 5 incidental resource stacks

If the character has only:
9 total slots

then:
9
- 3 equipment
- 2 food
- 2 mission
= 2 incidental categories

That is a huge gameplay difference.
The player isn't just carrying 33% more items.
They can participate in:
2 side economies
vs
5 side economies

during the same operation.
15. Generalizing beyond Extraction
This effect should apply to every archetype.
Explorer
Low inventory means:
founder carrots
OR specialty sapling
OR wool
OR discovered loot

Later:
bring all of them home

Developer
Low inventory means:
seed stock
+ food
+ maybe one animal-related resource

Later:
carry multiple founder crops,
breeding inputs,
bone meal,
special plants,
harvest surplus

Builder
Low inventory means:
commit to a material palette

Later:
carry several block families

Logistics
Low inventory creates very direct transport specialization.
Production
Low inventory limits how many inputs can be staged simultaneously.
So inventory is actually a universal economic-complexity progression stat.
That's stronger than treating it as generic convenience.
16. The resource matrix therefore needs two more fields
I would add:
Inventory Fragmentation Pressure
How many distinct stacks does normal exploitation tend to create?
Examples:
Coal:
low

Cow:
medium
beef + leather

Chicken:
high-ish
meat + feathers + eggs

Rabbit:
high relative to total value
meat + hide + foot

Mixed mining:
very high
coal + iron + copper + redstone + gravel + etc.

Mission Cargo Competition
How likely is this resource to compete with the player's primary payload?
Examples:
2 coal:
low individually,
but opens a whole stack category

96 copper:
very high payload footprint,
but likely mission-defining

carrot founder stock:
tiny physical footprint,
extremely high strategic value

This immediately makes rabbits even more interesting.
17. Rabbits become worse as opening economic prey than their food number suggests
A rabbit can yield multiple low-volume item types.
So early on:
small food value
+
hide stack
+
possible rabbit foot stack
+
awkward husbandry

creates poor inventory efficiency.
That reinforces the behavior we already wanted:
rabbit nearby
→ usually ignore
→ sometimes hunt when desperate
→ specialty value later

No nerf required.
Chickens are better, but even they create:
chicken
feathers
eggs

which means maintaining productive chickens may be more appealing than repeatedly slaughtering them when inventory is constrained.
Again, useful emergent behavior.
18. Founder resources have the opposite profile
Carrots are extremely inventory-efficient strategic capital.
A single stack can represent:
food
+
future crops
+
pig breeding
+
rabbit breeding

So their:
strategic value / slot

is enormous.
This helps explain why an Explorer should rationally prioritize them over much bulkier resources.
That is another matrix metric we can derive:
Strategic Density
Strategic Density
=
future systemic options enabled
/
inventory footprint

Not a literal balancing number necessarily, but conceptually:
carrot founder stock = very high
clay = low
copper = medium/high
diamond = extreme

This is different from food density or market value.
19. Updated resource-value model
We now have:
RESOURCE
│
├── Immediate Material Value
├── Founder / Capital Value
├── Processing Value
├── Producer Value
├── Systemic Importance
├── Strategic Density
├── Inventory Fragmentation
└── Current Match-State Value

and separately:
OPPORTUNITY
│
├── Signal strength
├── Search burden
├── Access burden
├── Exploitation burden
├── Return burden
├── Exposure
└── Diversion cost

This is getting much cleaner.
Resource and opportunity are finally not being conflated.
20. The main simulation conclusion
With the layered resource field plus inventory:
World contains more value
than player can efficiently consume.

That appears to be the desirable condition.
Not:
world contains exactly enough value
for player to collect everything

and not:
world contains mostly irrelevant clutter

The player's job is filtering.
That means a healthy region can produce:
many signals
some pickups
a few pockets
few strategic manifestations

while constrained inventory and time convert that abundance into actual choices.
The next rigorous piece I would lock onto is regeneration timing. Now that we know how players consume a region, we can calculate how many underlying candidate opportunities and what renewal interval are needed so that a region remains productive across a 30–60 minute match without replenishing fast enough for one player to settle into a single optimal loop.   




  







another factor i want to reintroduce- reduced durability for wood and stone. perhaps wood won't matter for genuine progression besides slightly slowing down the absolute early game, but stone tools should be seen as replaceable (wood tools should be expendable and not useful at all). a phase appropriate low level miner may have to either bring another stone pickaxe or be prepared to take time to craft a new one each time it breaks until iron tools, which have higher dura but also break over the compressed game time. what is the durability values that make this relationship meaningful













Yes. Durability can do useful work here because it creates another expedition-preparation constraint without inventing a new resource system.
I would balance durability around blocks of useful work per tool, not around “minutes until break.” Mining time varies too much with terrain, Haste, tool speed, what is being mined, and how much of an expedition is spent searching.
The relationship I would target
WOOD
bootstrap implement
→ obtain the materials for stone
→ discard it

STONE
consumable work tool
→ enough for one modest mining task
→ serious Extractor expects replacements

IRON
durable expedition equipment
→ enough for a substantial operation
→ still capable of breaking during normal match play

DIAMOND+
late-game persistence
→ durability increasingly stops being the central constraint

For reference, vanilla Java durability is approximately:
Pickaxe	Vanilla durability
Wood	59
Stone	131
Iron	250
Diamond	1,561
Netherite	2,031


Those values are wildly spaced relative to a compressed 30–60 minute match.
First sensitivity set
I would begin testing:
Tier	Test durability	Relative to vanilla	Intended meaning
Wood	12	20%	bootstrap only
Stone	48	37%	replaceable mining consumable
Iron	160	64%	real expedition tool
Diamond	leave unresolved initially	—	needs late-game economy context


I think 12 / 48 / 160 is the best first fixture.
Not canon yet.
Wood: ~12
A wood pick should not be an actual Extraction tool.
At 12 durability you can:
break enough stone to bootstrap
perhaps make a small mistake
possibly do a tiny amount of extra work

but you cannot reasonably say:
“I'll just keep using my wooden pick until it dies while mining.”

That's the desired behavior.
Even 16 would probably still work, but once we get much above ~20, the player starts receiving enough work from wood that its transitional nature becomes less absolute.
So I'd test:
WOOD = 12

acceptable sensitivity range:
8–16

Why not 3–5?
Because then we're essentially scripting:
wood pick
→ exactly mine enough cobblestone
→ throw away

That's overly gamey.
Twelve lets vanilla behavior breathe while making the conclusion obvious:
wood is not worth preserving.

Stone: ~48 is much more consequential
The desired stone relationship is not:
Stone pick = bad iron pick

It's:
Stone pick = expendable field equipment.

At 48 uses, think in terms of work budgets.
Small mining operation
For example:
8 blocks tunnel/access
+
20 ore/resource blocks
+
8 additional exposure blocks
=
36 durability

One stone pick handles it.
More substantial operation
20 access blocks
+
35 deposit blocks
+
15 exploratory/exposure blocks
=
70 durability

One does not.
The player needs:
2 stone picks

OR

1 stone pick
+ wood/sticks/stone
+ crafting access

That is exactly the relationship you described.
What carrying another stone pick actually means
This becomes especially interesting with constrained inventory.
A spare stone pick costs:
1 inventory slot

while field-crafting replacement tools requires some combination of:
sticks / wood
stone
crafting table access
crafting time
attention / safety

So the player chooses:
Prepared
Pickaxe
Spare Pickaxe
Food
Cargo...

Benefit:
no mining interruption

Cost:
-1 Economic Breadth Capacity

Improvised
Pickaxe
No spare
More cargo room

Benefit:
+1 usable slot

Cost:
tool breaks
→ stop
→ obtain/use materials
→ craft replacement
→ resume

That's an excellent early-game tradeoff.
And unlike an artificial “mining stamina” mechanic, it's just Minecraft.
How often should a stone pick break?
For a 6-minute Extraction operation, I would want something like:
light operation:
0–1 stone picks

normal dedicated mining operation:
1–2 stone picks

very mining-heavy operation:
2–3 stone picks

Not:
6 minutes = exactly one pick

because search-heavy and exploitation-heavy expeditions should differ.
A player who mostly prospects could return with their original pick.
A player who finds a major deposit should burn equipment.
That's useful feedback:
Finding something worth mining converts tool durability into material.

48 versus 64 stone durability
This is probably the important sensitivity comparison.
Stone = 64
A player gets:
33% more work/pick

than at 48.
Two picks provide:
128 blocks

which begins to feel like a pretty substantial expedition.
It makes spare tools relevant, but less reliably so.
Stone = 48
Two picks provide:
96 blocks

which is enough for a meaningful early mining operation without letting the player forget durability exists.
Given your intended relationship, I prefer 48.
Potential range:
STONE = 40–64
initial test = 48

Below ~32 I suspect it becomes irritating rather than strategic.
Iron: ~160
Iron should create a major qualitative transition.
From:
STONE
"I should bring another pick."

to:
IRON
"This pick should support the operation,
but it is still a consumable asset."

At 160 durability:
1 iron pick
≈ 3.3 stone picks at 48 durability

That's a very noticeable upgrade.
But it's nowhere near permanent.
A significant operation like:
40 access/exposure blocks
+
80 resource blocks
+
30 incidental excavation

= 150 durability

can actually consume the whole tool.
That means an Extractor discovering a rich region can plausibly experience:
enter with iron pick
→ conduct serious extraction
→ pick reaches low durability
→ decide whether to continue

instead of the iron pick simply becoming background equipment.
What does 160 mean over a match?
Consider relatively modest mining consumption:
Opening:
stone tools

Early-mid:
acquire iron pick

Then over match:
~400–600 meaningful block breaks

At 160 durability, that's roughly:
3–4 iron picks

if the player remained on iron.
That feels much more appropriate for a compressed match than vanilla's 250:
~2 iron picks

and enormously more appropriate than diamond's 1,561:
effectively permanent

for most match-scale activity.
The progression jump matters more than the precise values
Using the proposed numbers:
wood  12
stone 48
iron  160

we get:
WOOD → STONE
4× durability

STONE → IRON
3.33× durability

That's good because these aren't merely speed upgrades.
They change planning behavior.
Wood
"I need stone."

Stone
"How many picks should I bring?"

Iron
"How much serious mining can I get done before this wears out?"

That's a much better progression language.
It also interacts strongly with the ambient-resource simulation
Suppose our C-region Extractor sees:
2 coal
1 iron
small copper exposure
coal pocket
major copper manifestation

With a stone pick at 48 durability, the incidental pickups now cost two resources simultaneously:
TIME
+
TOOL DURABILITY

So vacuuming 20 ambient blocks isn't merely:
20 blocks gained

It's:
~42% of a stone pick consumed

before reaching the intended extraction opportunity.
That is significant.
New version of Diversion Cost
Earlier we had:
Diversion Cost =
path deviation
+ exploitation time
+ inventory footprint
+ uncertainty
+ return consequences

Now add:
+ tool consumption

So an insignificant coal pocket might be rejected because:
coal value: moderate

but:
time cost
+ one inventory category
+ 10 durability

isn't worth it.
Again:
no anti-vacuum rule is necessary.
The physical Minecraft economy creates the tradeoff.
Tool durability also makes ore value context-sensitive
Imagine 11 durability remaining.
The Extractor encounters:
8 coal

but believes the copper manifestation is nearby.
Suddenly those eight coal aren't merely competing for time.
They threaten:
pick breaks
→ replacement required
→ copper operation interrupted

So:
same coal deposit
same player
same resource economy

different remaining durability
→ different rational decision

That's excellent.
Remaining tool life becomes part of expedition state.
Stone replacement logistics
There's another nice consequence.
A replacement stone pick needs:
3 stone
2 sticks

The stone itself is usually trivial to acquire.
The more interesting input is effectively wood access.
So a prepared Extractor might carry:
sticks

instead of an entire spare pick.
Compare:
Spare stone pick
1 slot
48 ready durability
instant replacement

Stack of sticks
1 slot
many future tool handles
but requires local stone + crafting

Wood/logs
Even more flexible:
tools
crafting table
construction
fuel possibilities

This turns a basic Minecraft resource into expedition infrastructure.
And a crafting table becomes meaningful
If ordinary inventory crafting remains vanilla 2×2, pickaxes require a crafting table.
So the choice can become:
bring spare picks

OR

bring/place a crafting table

OR

know where a nearby workshop/cache exists

OR

return

That starts connecting Extraction to emergent organization without requiring recognized infrastructure.
A small forward mining camp containing:
crafting table
chest
wood
spare picks
food

has genuine value before the game recognizes it as any MOBA system.
That is precisely the layer separation we've been trying to protect.
Iron should still break
I strongly agree with that part.
If iron becomes effectively permanent, then the entire equipment-preparation economy disappears exactly when Extraction becomes most interesting.
At 160, an iron pick is durable enough that the player doesn't constantly think about it, but an Extractor doing serious mining should absolutely consume multiple across a match.
Possible sensitivity:
IRON
128 = highly consumable
160 = recommended initial fixture
192 = more forgiving
250 = vanilla; probably too durable for the target

My expectation is:
- 128 may still feel like “premium stone.”
- 192 might be viable if Extraction throughput is high.
- 160 is the useful middle test.
There is one major caveat: Unbreaking
This needs to be in the model from the start.
Vanilla Unbreaking dramatically changes expected effective tool life.
Roughly, for tools:
Unbreaking I   ≈ 2× expected effective durability
Unbreaking II  ≈ 3×
Unbreaking III ≈ 4×

So:
160 iron
+ Unbreaking III
≈ ~640 expected block uses

Suddenly the pick goes from:
expedition asset

to:
probably persistent across a large chunk of the match

That isn't necessarily bad.
It could make enchanting itself the progression mechanism that eventually removes tool logistics:
early:
stone replacement economy

mid:
iron durability economy

later:
enchanted equipment increasingly persistent

That is quite elegant.
But it means we should not separately tune late-game base durability without accounting for enchantments.
Mending is much more dangerous
Unbreaking extends the cycle.
Mending can potentially destroy it.
If experience access allows a player to continually repair the tool, then:
durability resource economy
→ largely disappears

Depending on how XP orbs/enchantments are handled in the MOBA, Mending may need special consideration.
I wouldn't solve that now, but it should be flagged before durability values are canonized.
Suggested first durability fixture
I would use:
Tool	Durability	Intended operational identity
Wood	12	bootstrap/disposable
Stone	48	replaceable early work tool
Iron	160	substantial but consumable expedition tool
Gold	defer	unusual speed/durability relationship
Diamond	defer	late-game system needs modeling
Netherite	defer	same


And define the intended behaviors rather than merely the numbers:
**Wooden tools exist to bootstrap the player into stone and should almost never be rational expedition equipment. Stone tools are replaceable consumables; dedicated early Extraction should expect to exhaust them. Iron tools are a major persistence upgrade





  







i think in the repo this durability ida might already present somewhere, or if not hopefully in chat context somewhere













Yes. I found the earlier discussion, and it changes the baseline we should use.
The repo/files I can search don't appear to contain the durability values themselves; the relevant current docs mostly preserve broader equipment/Extraction economy. For example, the handoff says Extraction includes tool efficiency and difficult-material access, and separately establishes iron as the ordinary sustainable combat-equipment ceiling, but it doesn't specify compressed tool durability. chathandoff.txtTXT
The prior chat discussion is much more specific. On September 10, we had already adopted the principle that durability stays, but is compressed to match timescale: equipment should actually wear out and require replacement/repair during a match, without turning gameplay into constant maintenance. The intended tier language was:
Wood feels temporary. Stone feels disposable. Iron feels reliable.

We also explicitly wanted a sawtoothed durability hierarchy rather than simply scaling every vanilla tier proportionally: wood extremely disposable, stone cheap/disposable, iron dependable, gold exceptionally fragile, diamond a durable investment, netherite premier staying power. 
Importantly, there was already an illustrative numerical fixture:
Tier	Earlier illustrative durability
Wood	20
Stone	35
Iron	60
Diamond	150
Netherite	200


Those numbers were explicitly illustrative, not balance proposals. But they show that my new 12 / 48 / 160 fixture drifted substantially from our previous conception—especially iron. The earlier model imagined iron as reliable relative to stone while still wearing out readily on match timescale, whereas 160 starts moving much closer to “one iron pick handles a whole substantial operation.”
So I would retract 12 / 48 / 160 as our starting point.
The new simulation gives us better tools for calibrating the old idea rather than replacing it. The relationship we actually need to test is something like:
WOOD
temporary bootstrap
durability almost irrelevant economically
because replacement by stone is immediate

STONE
disposable Extraction equipment
one meaningful operation can consume multiple picks
spare/replacement preparation matters

IRON
reliable Extraction equipment
usually survives an ordinary operation
but dedicated mining over multiple operations consumes it

DIAMOND
durable strategic investment
durability itself is part of what the diamond expenditure buys

NETHERITE
premier persistence

And now we can quantify that against actual expected block consumption, including ambient pickups + access excavation + strategic manifestation extraction.
The critical comparison is therefore probably not 48 stone vs 160 iron. It is closer to the previously imagined ~35 stone vs ~60–90 iron range. At 35 / 70, for example, iron is exactly twice as durable: enough to remove the constant spare-pick concern without removing durability from the expedition economy.
That also makes the choice to carry a spare stone pick much more meaningful. A 35-durability stone tool means an Extractor expecting, say,
10 access/exposure blocks
+ 5 incidental blocks
+ 25 deposit blocks
= 40 durability demand

already knows one stone pick is insufficient. They either carry another, carry replacement inputs, use a forward cache/workshop, or accept interruption.
An iron pick around 70–80 would handle that same operation comfortably, but a second substantial operation could break it. That sounds much closer to “Stone feels disposable; Iron feels reliable” than 160 does.
It also preserves the new rational-actor effect beautifully: mining two incidental coal blocks isn't just two blocks of time and a coal stack. On a 35-durability stone pick, it consumes ~5.7% of the player's current tool. Ten incidental blocks consume ~29%. On an 80-durability iron pick, the same ten blocks consume only 12.5%. Tool progression therefore makes ambient resources progressively cheaper to accept, alongside inventory progression making them progressively easier to carry.
So we now have two independent reasons the constrained early player becomes a more complete Minecraft player:
INVENTORY PROGRESSION
→ can afford more simultaneous resource categories

TOOL PROGRESSION
→ can afford more incidental extraction
   without compromising the primary operation
That is stronger than what we had when durability was discussed originally.
For the rigorous pass, I'd therefore restore the old design as the governing premise and test around Wood 12–20 / Stone 32–40 / Iron 64–96, with 16 / 36 / 80 as a useful central fixture—not because those are preferred outcomes, but because that range actually brackets the previously established relationship. Then we can derive whether 36/80, 35/70, or something else produces the intended replacement frequency under the resource simulation rather than guessing durability in isolation.