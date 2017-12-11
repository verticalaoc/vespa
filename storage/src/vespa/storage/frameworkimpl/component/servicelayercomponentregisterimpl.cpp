// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#include "servicelayercomponentregisterimpl.h"
#include <vespa/storage/storageserver/configurable_bucket_resolver.h>
#include <vespa/storage/common/global_bucket_space_distribution_converter.h>

#include <vespa/config/config.h>
#include <vespa/config/print/asciiconfigreader.h> // TODO temp

#include <vespa/vespalib/util/exceptions.h>

namespace storage {

using vespalib::IllegalStateException;

ServiceLayerComponentRegisterImpl::ServiceLayerComponentRegisterImpl()
    : _diskCount(0),
      _bucketSpaceRepo()
{ }

void
ServiceLayerComponentRegisterImpl::registerServiceLayerComponent(
        ServiceLayerManagedComponent& smc)
{
    vespalib::LockGuard lock(_componentLock);
    _components.push_back(&smc);
    smc.setDiskCount(_diskCount);
    smc.setBucketSpaceRepo(_bucketSpaceRepo);
    smc.setMinUsedBitsTracker(_minUsedBitsTracker);
}

void
ServiceLayerComponentRegisterImpl::setDiskCount(uint16_t count)
{
    vespalib::LockGuard lock(_componentLock);
    if (_diskCount != 0) {
        throw IllegalStateException("Disk count already set. Cannot be updated live", VESPA_STRLOC);
    }
    _diskCount = count;
    for (uint32_t i=0; i<_components.size(); ++i) {
        _components[i]->setDiskCount(count);
    }
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


void
ServiceLayerComponentRegisterImpl::setDistribution(lib::Distribution::SP distribution)
{
    // For now, copy distribution to all content bucket spaces
    for (const auto &elem : _bucketSpaceRepo) {
        elem.second->setDistribution(distribution);
    }
    _bucketSpaceRepo.get(FixedBucketSpaces::default_space()).setDistribution(distribution);
    if (_bucketSpaceRepo.hasGlobalBucketSpace()) {
        auto global_distr_config = GlobalBucketSpaceDistributionConverter::convert_to_global(
                *string_to_config(distribution->serialize()));
        auto global_distr = std::make_shared<const lib::Distribution>(*global_distr_config);
        _bucketSpaceRepo.get(FixedBucketSpaces::global_space()).setDistribution(std::move(global_distr));
    }
    // FIXME this is not space aware!
    StorageComponentRegisterImpl::setDistribution(distribution);
}

} // storage
