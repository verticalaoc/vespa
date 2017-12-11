// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#include <vespa/storage/storageserver/configurable_bucket_resolver.h>
#include "content_bucket_space_repo.h"

using document::BucketSpace;

namespace storage {

ContentBucketSpaceRepo::ContentBucketSpaceRepo()
    : _map()
{
    _map.emplace(FixedBucketSpaces::default_space(), std::make_unique<ContentBucketSpace>(FixedBucketSpaces::default_space()));
}

void ContentBucketSpaceRepo::enableGlobalBucketSpace() {
    _map.emplace(FixedBucketSpaces::global_space(), std::make_unique<ContentBucketSpace>(FixedBucketSpaces::global_space()));
}

bool ContentBucketSpaceRepo::hasGlobalBucketSpace() const {
    return (_map.find(FixedBucketSpaces::global_space()) != _map.end());
}

ContentBucketSpace &
ContentBucketSpaceRepo::get(BucketSpace bucketSpace) const
{
    //assert(bucketSpace == BucketSpace::placeHolder());
    auto itr = _map.find(bucketSpace);
    assert(itr != _map.end());
    return *itr->second;
}

ContentBucketSpaceRepo::BucketSpaces
ContentBucketSpaceRepo::getBucketSpaces() const
{
    BucketSpaces result;
    for (const auto &elem : _map) {
        result.push_back(elem.first);
    }
    return result;
}

size_t
ContentBucketSpaceRepo::getBucketMemoryUsage() const
{
    size_t result = 0;
    for (const auto &elem : _map) {
        result += elem.second->bucketDatabase().getMemoryUsage();
    }
    return result;
}

}
