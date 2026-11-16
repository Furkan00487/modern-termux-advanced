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

}  // namespace

int main() {
    testRejectsGpuOnlyFlip();
    testRejectsExternalRgbaAhbFlip();
    testAllowsInternalAhbFlip();
    testAllocatesAndReusesCpuFallbackForGpuOnlyAccess();
    testPrefersLockedPointerWithoutAllocatingFallback();
    std::printf("dri3_safety_test: all 5 assertions passed\n");
    return 0;
}
