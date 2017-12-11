// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#include "distributor_bucket_space_repo.h"
#include "distributor_bucket_space.h"
#include <vespa/vdslib/distribution/distribution.h>
#include <vespa/storage/storageserver/configurable_bucket_resolver.h> // TODO move

#include <vespa/config/config.h>
#include <vespa/config/print/asciiconfigreader.h> // TODO temp

#include <vespa/storage/common/global_bucket_space_distribution_converter.h>
#include <cassert>

#include <vespa/log/log.h>
LOG_SETUP(".distributor.distributor_bucket_space_repo");

using document::BucketSpace;

namespace storage {
namespace distributor {

DistributorBucketSpaceRepo::DistributorBucketSpaceRepo()
    : _map()
{
    add(FixedBucketSpaces::default_space(), std::make_unique<DistributorBucketSpace>());
    // TODO enable global distribution by default?
}

DistributorBucketSpaceRepo::~DistributorBucketSpaceRepo() = default;

void
DistributorBucketSpaceRepo::add(document::BucketSpace bucketSpace, std::unique_ptr<DistributorBucketSpace> distributorBucketSpace)
{
    _map.emplace(bucketSpace, std::move(distributorBucketSpace));
}

void DistributorBucketSpaceRepo::enableGlobalDistribution() {
    if (!hasGlobalDistribution()) {
        add(FixedBucketSpaces::global_space(), std::make_unique<DistributorBucketSpace>());
    }
}

bool DistributorBucketSpaceRepo::hasGlobalDistribution() const noexcept {
    return (_map.find(FixedBucketSpaces::global_space()) != _map.end());
}

using DistributionConfig = vespa::config::content::StorDistributionConfig;

namespace {

// FIXME dupe with global_bucket_space_distribution_converter.cpp
std::unique_ptr<DistributionConfig> string_to_config(const vespalib::string& cfg) {
    vespalib::asciistream iss(cfg);
    config::AsciiConfigReader<vespa::config::content::StorDistributionConfig> reader(iss);
    return reader.read();
}

}

void DistributorBucketSpaceRepo::setDefaultDistribution(
        std::shared_ptr<const lib::Distribution> default_distr)
{
    LOG(debug, "Got new default distribution '%s'", default_distr->toString().c_str());
    if (hasGlobalDistribution()) {
        auto global_distr_config = GlobalBucketSpaceDistributionConverter::convert_to_global(
                *string_to_config(default_distr->serialize()));
        auto global_distr = std::make_shared<const lib::Distribution>(*global_distr_config);
        get(FixedBucketSpaces::global_space()).setDistribution(std::move(global_distr));
    }
    // TODO all spaces, per-space config transforms
    get(FixedBucketSpaces::default_space()).setDistribution(std::move(default_distr));

}

DistributorBucketSpace &
DistributorBucketSpaceRepo::get(BucketSpace bucketSpace)
{
    auto itr = _map.find(bucketSpace);
    assert(itr != _map.end());
    return *itr->second;
}

const DistributorBucketSpace &
DistributorBucketSpaceRepo::get(BucketSpace bucketSpace) const
{
    auto itr = _map.find(bucketSpace);
    assert(itr != _map.end());
    return *itr->second;
}

DistributorBucketSpace &
DistributorBucketSpaceRepo::getDefaultSpace() noexcept
{
    return get(BucketSpace::placeHolder());
}

const DistributorBucketSpace &
DistributorBucketSpaceRepo::getDefaultSpace() const noexcept
{
    return get(BucketSpace::placeHolder());
}

/*
 * Stuff hacked in:
 *  - default bucket space now 1, not 0
 *  - global space _always_ enabled in service layer
 *  - global space selectively enabled on distibutor based on config
 *  - config model validation of redundancy/ready copies is commented out
 * MISC TODOs:
 *  - visiting core dumps at the moment due to missing bucket space from client
 *  - config on startup is awkward
 *  - reconfig is even more awkward
 *  - need centralized knowledge of mapping of space name -> space id
 *  - get returns empty parent ref for child doc...?!
 */

}
}
