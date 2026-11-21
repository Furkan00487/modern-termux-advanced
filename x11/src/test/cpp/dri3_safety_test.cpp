#include <cassert>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <cstring>

#include "dri3_safety.h"

namespace {

int gAllocCalls = 0;

void *countingCalloc(size_t count, size_t size) {
    gAllocCalls++;
    return std::calloc(count, size);
}

void testRejectsGpuOnlyFlip() {
    // AFBC / non-lockable buffer must never become root/screen pixmap via Present flip.
    bool canFlip = lorieCanFlipBuffer(
        true,   // hasPriv
        true,   // hasBuffer
        false,  // hasMem
        true,   // gpuOnly (AFBC lock failed)
        true,   // sizeMatchesRoot
        3,      // LORIEBUFFER_AHARDWAREBUFFER
        2,      // AHARDWAREBUFFER_FORMAT_R8G8B8X8_UNORM
        true,   // imported
        false   // forceFlip
    );
    assert(canFlip == false);
}

void testRejectsExternalRgbaAhbFlip() {
    // External R8G8B8A8_UNORM (format 1) must go through GPU copy swizzle path, not flip as root.
    bool canFlip = lorieCanFlipBuffer(
        true, true, false, false, true,
        3,  // LORIEBUFFER_AHARDWAREBUFFER
        1,  // AHARDWAREBUFFER_FORMAT_R8G8B8A8_UNORM
        true, false
    );
    assert(canFlip == false);
}

void testAllowsInternalAhbFlip() {
    bool canFlip = lorieCanFlipBuffer(
        true, true, false, false, true,
        3,  // LORIEBUFFER_AHARDWAREBUFFER
        2,  // AHARDWAREBUFFER_FORMAT_R8G8B8X8_UNORM
        false, false
    );
    assert(canFlip == true);
}

void testAllocatesAndReusesCpuFallbackForGpuOnlyAccess() {
    gAllocCalls = 0;
    void *fallback = nullptr;

    void *ptr1 = lorieResolveCpuAccessPointer(nullptr, nullptr, &fallback, 1088, 1892, countingCalloc);
    assert(ptr1 != nullptr);
    assert(fallback == ptr1);
    assert(gAllocCalls == 1);

    // Calling again reuses existing fallback instead of leaking.
    void *ptr2 = lorieResolveCpuAccessPointer(nullptr, nullptr, &fallback, 1088, 1892, countingCalloc);
    assert(ptr2 == ptr1);
    assert(gAllocCalls == 1);

    std::free(fallback);
}

void testPrefersLockedPointerWithoutAllocatingFallback() {
    gAllocCalls = 0;
    uint32_t dummyPixel = 0x11223344;
    void *fallback = nullptr;

    void *ptr = lorieResolveCpuAccessPointer(&dummyPixel, nullptr, &fallback, 1088, 1892, countingCalloc);
    assert(ptr == &dummyPixel);
    assert(fallback == nullptr);
    assert(gAllocCalls == 0);
}

void testShouldRecordExternalMemFiltersInternalSync() {
    int dummyLocked = 1;
    int dummyFallback = 2;
    int dummyExternal = 3;

    // Internal EXA sys_ptr sync must never pollute priv->mem.
    assert(lorieShouldRecordExternalMem(true, &dummyExternal, true, &dummyLocked, &dummyFallback) == false);
    assert(lorieShouldRecordExternalMem(true, &dummyFallback, false, &dummyLocked, &dummyFallback) == false);
    assert(lorieShouldRecordExternalMem(true, &dummyLocked, false, &dummyLocked, &dummyFallback) == false);
    // Genuine external scratch pixmap pointer must be recorded.
    assert(lorieShouldRecordExternalMem(true, &dummyExternal, false, &dummyLocked, &dummyFallback) == true);
    assert(lorieShouldRecordExternalMem(true, nullptr, false, nullptr, nullptr) == false);
}

void testZeroReturnWithNullPointerCountsAsFailedLock() {
    // Samsung gralloc4 on AFBC buffers can log an error and return 0 while leaving vaddr NULL.
    assert(lorieLockSucceeded(0, nullptr) == false);
    assert(lorieLockSucceeded(-1, reinterpret_cast<const void *>(0x1234)) == false);

    int dummy = 42;
    assert(lorieLockSucceeded(0, &dummy) == true);

    void *lockedData = nullptr;
    int8_t lockedFlag = 1;
    int err = lorieNormalizeLockResult(0, &lockedData, &lockedFlag, 13 /* EACCES */);
    assert(err == 13);
    assert(lockedData == nullptr);
    assert(lockedFlag == 0);

    // Non-zero error with stale pointer must also clear lockedData and lockedFlag.
    lockedData = &dummy;
    lockedFlag = 1;
    err = lorieNormalizeLockResult(-22, &lockedData, &lockedFlag, 13);
    assert(err == -22);
    assert(lockedData == nullptr);
    assert(lockedFlag == 0);

    // Only err == 0 with non-null data succeeds and marks lockedFlag = 1.
    lockedData = &dummy;
    lockedFlag = 0;
    err = lorieNormalizeLockResult(0, &lockedData, &lockedFlag, 13);
    assert(err == 0);
    assert(lockedData == &dummy);
    assert(lockedFlag == 1);
}

void testComputePixelBufferBytesPreventsIntOverflowAndBadDims() {
    size_t bytes = 123;

    // Invalid inputs must fail without modifying outBytes.
    assert(lorieComputePixelBufferBytes(0, 1080, &bytes) == false);
    assert(bytes == 123);
    assert(lorieComputePixelBufferBytes(-1, 1080, &bytes) == false);
    assert(lorieComputePixelBufferBytes(1920, 0, &bytes) == false);
    assert(lorieComputePixelBufferBytes(1920, -4, &bytes) == false);
    assert(lorieComputePixelBufferBytes(1920, 1080, nullptr) == false);

    // Normal desktop buffer size.
    assert(lorieComputePixelBufferBytes(1920, 1080, &bytes) == true);
    assert(bytes == 1920u * 1080u * 4u);

    // Dimensions whose product overflows 32-bit signed int (46341 * 46341 > INT32_MAX):
    // must succeed without 32-bit UB on 64-bit size_t, and reject on 32-bit size_t.
    bool largeOk = lorieComputePixelBufferBytes(46341, 46341, &bytes);
    if (sizeof(size_t) >= 8) {
        assert(largeOk == true);
        assert(bytes == static_cast<size_t>(46341) * static_cast<size_t>(46341) * 4u);
    } else {
        assert(largeOk == false);
    }
}

}  // namespace

int main() {
    testRejectsGpuOnlyFlip();
    testRejectsExternalRgbaAhbFlip();
    testAllowsInternalAhbFlip();
    testAllocatesAndReusesCpuFallbackForGpuOnlyAccess();
    testPrefersLockedPointerWithoutAllocatingFallback();
    testShouldRecordExternalMemFiltersInternalSync();
    testZeroReturnWithNullPointerCountsAsFailedLock();
    testComputePixelBufferBytesPreventsIntOverflowAndBadDims();
    std::printf("dri3_safety_test: all 8 assertions passed\n");
    return 0;
}
