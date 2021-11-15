// Copyright Yahoo. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#include <vespa/searchcore/proton/documentmetastore/lid_allocator.h>
#include <vespa/vespalib/util/generationholder.h>
#include <vespa/vespalib/gtest/gtest.h>

using vespalib::GenerationHolder;

namespace proton {

using documentmetastore::LidAllocator;

class LidAllocatorTest : public ::testing::Test
{
protected:
    GenerationHolder _gen_hold;
    LidAllocator     _allocator;

    LidAllocatorTest()
        : ::testing::Test(),
          _gen_hold(),
          _allocator(100, 100, _gen_hold)
    {
    }

    ~LidAllocatorTest()
    {
        _gen_hold.clearHoldLists();
    }

    uint32_t get_size() { return _allocator.getActiveLids().size(); }

    void construct_free_list() {
        _allocator.constructFreeList(_allocator.getActiveLids().size());
        _allocator.setFreeListConstructed();
    }

    void register_lids(const std::vector<uint32_t>& lids) {
        for (uint32_t lid : lids) {
            _allocator.registerLid(lid);
        }
    }

    std::vector<uint32_t> alloc_lids(uint32_t count) {
        std::vector<uint32_t> result;
        for (uint32_t i = 0; i < count; ++i) {
            result.emplace_back(_allocator.getFreeLid(get_size()));
        }
        return result;
    }

    void activate_lids(const std::vector<uint32_t>& lids, bool active) {
        for (uint32_t lid : lids) {
            _allocator.updateActiveLids(lid, active);
        }
    }
    
    void unregister_lids(const std::vector<uint32_t>& lids) {
         _allocator.unregister_lids(lids);
    }

    void hold_lids(const std::vector<uint32_t>& lids) {
        _allocator.holdLids(lids, get_size(), 0);
    }

    void trim_hold_lists() {
        _allocator.trimHoldLists(1);
    }
    
    std::vector<uint32_t> get_valid_lids() {
        std::vector<uint32_t> result;
        auto size = get_size();
        for (uint32_t lid = 1; lid < size; ++lid) {
            if (_allocator.validLid(lid)) {
                result.emplace_back(lid);
            }
        }
        return result;
    }

    std::vector<uint32_t> get_active_lids() {
        std::vector<uint32_t> result;
        auto active_lids = _allocator.getActiveLids();
        uint32_t lid = active_lids.getNextTrueBit(1);
        while (lid < active_lids.size()) {
            if (active_lids.testBit(lid)) {
                result.emplace_back(lid);
            }
            lid = active_lids.getNextTrueBit(lid + 1);
        }
        return result;
    }

    void
    assert_valid_lids(const std::vector<uint32_t>& exp_lids) {
        EXPECT_EQ(exp_lids, get_valid_lids());
    }

    void
    assert_active_lids(const std::vector<uint32_t>& exp_lids) {
        EXPECT_EQ(exp_lids, get_active_lids());
    }

};

TEST_F(LidAllocatorTest, unregister_lids)
{
    register_lids({ 1, 2, 3, 4, 5, 6 });
    activate_lids({ 4, 5, 6 }, true);
    assert_valid_lids({1, 2, 3, 4, 5, 6});
    assert_active_lids({4, 5, 6});
    construct_free_list();
    unregister_lids({1, 3, 5});
    assert_valid_lids({2, 4, 6});
    assert_active_lids({4, 6});
    hold_lids({1, 3, 5});
    trim_hold_lists();
    EXPECT_EQ((std::vector<uint32_t>{1, 3, 5, 7, 8}), alloc_lids(5));
}

}

GTEST_MAIN_RUN_ALL_TESTS()
