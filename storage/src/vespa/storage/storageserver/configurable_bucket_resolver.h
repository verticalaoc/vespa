// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
#pragma once

#include <vespa/storage/config/config-bucketspaces.h>
#include <vespa/storage/common/bucket_resolver.h>
#include <vespa/vespalib/stllike/hash_fun.h>
#include <memory>
#include <unordered_map>

namespace storage {

// TODO move to new home. Persistence?
struct FixedBucketSpaces {
    static constexpr document::BucketSpace default_space() { return document::BucketSpace(1); };
    static constexpr document::BucketSpace global_space() { return document::BucketSpace(2); }

    // Post-condition: returned space has valid() == true iff name
    // is either "default" or "global".
    static document::BucketSpace from_string(vespalib::stringref name) {
        if (name == "default") {
            return default_space();
        } else if (name == "global") {
            return global_space();
        } else {
            return document::BucketSpace::makeInvalid();
        }
    }

    static vespalib::stringref to_string(document::BucketSpace space) {
        if (space == default_space()) {
            return "default";
        } else if (space == global_space()) {
            return "global";
        } else {
            return "INVALID";
        }
    }
};

class ConfigurableBucketResolver : public BucketResolver {
public:
    using BucketSpaceMapping = std::unordered_map<vespalib::string, document::BucketSpace, vespalib::hash<vespalib::string>>;
    const BucketSpaceMapping _type_to_space;
public:
    explicit ConfigurableBucketResolver(BucketSpaceMapping type_to_space)
        : _type_to_space(std::move(type_to_space))
    {}

    document::Bucket bucketFromId(const document::DocumentId&) const override;
    document::BucketSpace bucketSpaceFromName(const vespalib::string& name) const override;
    vespalib::string nameFromBucketSpace(const document::BucketSpace& space) const override;

    static std::shared_ptr<ConfigurableBucketResolver> from_config(
            const vespa::config::content::core::BucketspacesConfig& config);
};

}