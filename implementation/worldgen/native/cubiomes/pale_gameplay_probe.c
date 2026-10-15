/* Local biome/approximate-height grid for Pale Forest gameplay screening.
 * SEED CENTRE_X CENTRE_Z HALF STEP OUT; no chunk generation or world writes.
 */
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include "generator.h"
#include "biomenoise.h"

int main(int argc,char **argv){
    if(argc!=7){fprintf(stderr,"SEED CENTRE_X CENTRE_Z HALF STEP OUT\n");return 2;}
    uint64_t seed=strtoull(argv[1],0,10);
    int cx=atoi(argv[2]),cz=atoi(argv[3]),half=atoi(argv[4]),step=atoi(argv[5]);
    if(step<=0||half<=0||2*half%step)return 2;
    int n=2*half/step;
    Generator g;setupGenerator(&g,MC_1_21_11,0);applySeed(&g,DIM_OVERWORLD,seed);
    SurfaceNoise sn;initSurfaceNoise(&sn,DIM_OVERWORLD,seed);
    int16_t *body=malloc((size_t)n*n*2*sizeof(int16_t));if(!body)return 3;
    for(int j=0;j<n;j++)for(int i=0;i<n;i++){
        int x=cx-half+i*step,z=cz-half+j*step;float y=0;int unused=0;
        mapApproxHeight(&y,&unused,&g,&sn,x>>2,z>>2,1,1);
        body[(j*n+i)*2]=(int16_t)(y<0?y-.5f:y+.5f);
        body[(j*n+i)*2+1]=(int16_t)getBiomeAt(&g,4,x>>2,319>>2,z>>2);
    }
    FILE *out=fopen(argv[6],"wb");if(!out){free(body);return 4;}
    fprintf(out,"palegameplay 1 %llu %d %d %d %d %d\n",(unsigned long long)seed,n,cx,cz,half,step);
    size_t expected=(size_t)n*n*2,actual=fwrite(body,sizeof(int16_t),expected,out);
    fclose(out);free(body);return actual==expected?0:5;
}
