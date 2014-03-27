#ifdef USE_BOEHM_GC
#include "gc.h"
#endif

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>

#ifdef USE_BOEHM_GC
#define MJ_MALLOC GC_MALLOC
#else
#define MJ_MALLOC malloc
#endif

/* entry point provided by the compiler */
void mj_main();

int main()
{
#ifdef USE_BOEHM_GC
    GC_INIT();
#endif
    mj_main();
    return 0;
}

void *minijavaNew(void *vtbl, unsigned long szWords)
{
    unsigned long sz = szWords * sizeof(void *);
    void **ret = (void **) MJ_MALLOC(sz);
    if (ret == NULL) {
        perror("MJ_MALLOC");
        exit(1);
    }
    ret[0] = vtbl;
#ifndef USE_BOEHM_GC
    memset(ret + 1, 0, sz - sizeof(void *));
#endif
    return ret;
}

void *minijavaNewArray(unsigned long szWords)
{
    unsigned long sz = (szWords+1) * sizeof(void *);
    void **ret = (void **) MJ_MALLOC(sz);
    if (ret == NULL) {
        perror("MJ_MALLOC");
        exit(1);
    }
    ret[0] = (void *) szWords;
#ifndef USE_BOEHM_GC
    memset(ret + 1, 0, sz - sizeof(void *));
#endif
    return ret;
}

long minijavaPrint(long value)
{
    printf("%ld\n", value);
}
