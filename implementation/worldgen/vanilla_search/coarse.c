/* Biome-only rejection/ranking. Never a terrain generator.
 * Build against pinned Cubiomes with -O3 -fwrapv. JSONL on stdout, counts stderr.
 */
#include "generator.h"
#include "util.h"
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <inttypes.h>
#include <time.h>
#include <math.h>

static int high(int b) { return b==meadow || b==grove || b==snowy_slopes || b==jagged_peaks || b==frozen_peaks || b==stony_peaks || b==windswept_hills || b==windswept_gravelly_hills; }
static int alpine(int b) { return b==grove || b==snowy_slopes || b==jagged_peaks || b==frozen_peaks; }
static int openland(int b) { return b==plains || b==sunflower_plains || b==meadow || b==savanna || b==snowy_plains; }
static int woods(int b) { return b==forest || b==birch_forest || b==dark_forest || b==taiga || b==snowy_taiga || b==old_growth_pine_taiga || b==old_growth_spruce_taiga || b==jungle || b==bamboo_jungle || b==pale_garden || b==cherry_grove || b==grove; }
static double now(void) { struct timespec t; timespec_get(&t,TIME_UTC); return t.tv_sec+t.tv_nsec/1e9; }
int main(int argc,char **argv) {
    if(argc==2 && !strcmp(argv[1],"--refine")) {
        Generator g;setupGenerator(&g,MC_1_21,0);int64_t seed,last=INT64_MIN;int cx,cz,rotation;
        while(scanf("%" SCNd64 " %d %d %d",&seed,&cx,&cz,&rotation)==4) {
            if(seed!=last){applySeed(&g,DIM_OVERWORLD,(uint64_t)seed);last=seed;}
            int w=rotation%180?33:27,h=rotation%180?27:33;double temperate=0,center=0,snowplain=0,peak=0;int nc=0,nw=0;
            for(int z=0;z<h;z++)for(int x=0;x<w;x++) {
                int rx,rz;
                if(rotation==0){rx=x;rz=z;}else if(rotation==90){rx=z;rz=32-x;}else if(rotation==180){rx=26-x;rz=32-z;}else{rx=26-z;rz=x;}
                int b=getBiomeAt(&g,4,(cx-416+rx*32)/4,24,(cz-512+rz*32)/4);
                int warm=b==plains||b==sunflower_plains||b==meadow||b==savanna;
                temperate+=warm;snowplain+=b==snowy_plains;
                if(x>=w/3&&x<2*w/3&&z>=h/3&&z<2*h/3){nc++;center+=warm;}
                if(x<w/3){nw++;peak+=b==snowy_slopes||b==jagged_peaks||b==frozen_peaks||b==stony_peaks;}
            }
            printf("%.6f %.6f %.6f %.6f\n",temperate/891,center/nc,snowplain/891,peak/nw);
        }
        return 0;
    }
    if(argc==2 && !strcmp(argv[1],"--probe")) {
        Generator g; setupGenerator(&g,MC_1_21,0);
        int64_t seed,last=INT64_MIN; int x,y,z;
        while(scanf("%" SCNd64 " %d %d %d",&seed,&x,&y,&z)==4) {
            if(seed!=last){applySeed(&g,DIM_OVERWORLD,(uint64_t)seed);last=seed;}
            /* Compare stored quart biome palettes, not block Voronoi lookup. */
            puts(biome2str(MC_1_21,getBiomeAt(&g,4,(int)floor(x/4.0),(int)floor(y/4.0),(int)floor(z/4.0))));
        }
        return 0;
    }
    if(argc!=3) { fprintf(stderr,"usage: coarse START_SEED SEED_COUNT\n"); return 2; }
    int64_t start=strtoll(argv[1],0,10); int count=atoi(argv[2]);
    if(count<1) return 2;
    Generator g; setupGenerator(&g,MC_1_21,0);
    const int centers[5][2]={{0,0},{2048,0},{-2048,0},{0,2048},{0,-2048}};
    long searched=0,passed=0,ranked=0,reject_ocean=0,reject_high=0,reject_land=0;
    double ta=0,tb=0,begin=now();
    for(int s=0;s<count;s++) {
        double t=now(); applySeed(&g,DIM_OVERWORLD,(uint64_t)(start+s)); ta+=now()-t;
        for(int region=0;region<5;region++) {
            int cx=centers[region][0],cz=centers[region][1],o=0,h=0;
            t=now(); searched++;
            for(int z=-4;z<=4;z++) for(int x=-3;x<=3;x++) {
                int b=getBiomeAt(&g,4,(cx+x*128)/4,24,(cz+z*128)/4);
                o+=isOceanic(b); h+=high(b);
            }
            ta+=now()-t;
            if(!o) {reject_ocean++;continue;}
            if(!h) {reject_high++;continue;}
            if(o>48) {reject_land++;continue;}
            passed++; t=now();
            int grid[33][27],hist[256]={0};
            for(int z=0;z<33;z++) for(int x=0;x<27;x++) {
                int b=getBiomeAt(&g,4,(cx-416+x*32)/4,24,(cz-512+z*32)/4);
                grid[z][x]=b; if(b>=0&&b<256) hist[b]++;
            }
            double best=-1e9; int bestrot=0; double features[8]={0};
            for(int r=0;r<4;r++) {
                int w=r%2?33:27,ht=r%2?27:33;
                double wo=0,eo=0,wh=0,eh=0,wa=0,op=0,fo=0,hn=0,hs=0,co=0;
                int nw=0,ne=0,nhn=0,nhs=0,nc=0;
                for(int z=0;z<ht;z++) for(int x=0;x<w;x++) {
                    int rx,rz;
                    if(r==0){rx=x;rz=z;} else if(r==1){rx=z;rz=32-x;} else if(r==2){rx=26-x;rz=32-z;} else{rx=26-z;rz=x;}
                    int b=grid[rz][rx]; op+=openland(b); fo+=woods(b);
                    if(x<w/3){nw++;wo+=isOceanic(b);wh+=high(b);wa+=alpine(b);}
                    if(x>=2*w/3){ne++;eo+=isOceanic(b);eh+=high(b);}
                    if(x>=w/4&&x<3*w/4) {
                        if(z<ht/3){nhn++;hn+=!isOceanic(b)&&!high(b);}
                        if(z>=2*ht/3){nhs++;hs+=!isOceanic(b)&&!high(b);}
                        if(z>=ht/3&&z<2*ht/3){nc++;co+=openland(b);}
                    }
                }
                wo/=nw;eo/=ne;wh/=nw;eh/=ne;wa/=nw;op/=891;fo/=891;hn/=nhn;hs/=nhs;co/=nc;
                /* Three viability conditions, other composition is ranking only. */
                if(eo<.04 || eo<=wo || wh<.025 || hn<.30 || hs<.30) continue;
                double score=30*(eo-wo)+25*(wh-eh)+18*wa+18*op+12*co+8*(hn<hs?hn:hs)+8*(fo<.25?fo:.25)-20*(fo>.55?fo-.55:0);
                if(score>best) {best=score;bestrot=r*90;features[0]=eo;features[1]=wh;features[2]=wa;features[3]=op;features[4]=fo;features[5]=hn;features[6]=hs;features[7]=co;}
            }
            tb+=now()-t;
            if(best< -1e8) continue;
            ranked++;
            printf("{\"seed\":%" PRId64 ",\"center\":[%d,%d],\"rotation\":%d,\"score\":%.5f,\"features\":[",start+s,cx,cz,bestrot,best);
            for(int k=0;k<8;k++) printf("%s%.5f",k?",":"",features[k]);
            printf("]}\n");
        }
    }
    fprintf(stderr,"{\"seeds\":%d,\"windows\":%ld,\"stage_a_survivors\":%ld,\"stage_b_survivors\":%ld,\"stage_a_rejections\":{\"no_ocean_sample\":%ld,\"no_highland_opportunity\":%ld,\"mostly_ocean\":%ld},\"stage_a_seconds\":%.6f,\"stage_b_seconds\":%.6f,\"wall_seconds\":%.6f}\n",count,searched,passed,ranked,reject_ocean,reject_high,reject_land,ta,tb,now()-begin);
    return 0;
}
