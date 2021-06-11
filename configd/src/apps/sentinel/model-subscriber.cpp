// Copyright Verizon Media. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#include "model-subscriber.h"
#include <vespa/vespalib/util/exceptions.h>
#include <vespa/config/common/exceptions.h>
#include <string>
#include <chrono>
#include <vespa/log/log.h>

LOG_SETUP(".sentinel.model-subscriber");

using namespace std::chrono_literals;

namespace config::sentinel {

std::optional<ModelConfig> ModelSubscriber::getModelConfig() {
    checkForUpdates();
    if (_modelConfig) {
        return ModelConfig(*_modelConfig);
    } else {
        return {};
    }
}


ModelSubscriber::ModelSubscriber(const std::string &configId)
  : _configId(configId)
{}

ModelSubscriber::~ModelSubscriber() = default;

void
ModelSubscriber::start(std::chrono::milliseconds timeout) {
    try {
        _modelHandle =_subscriber.subscribe<ModelConfig>(_configId, timeout);
    } catch (ConfigTimeoutException & ex) {
        LOG(warning, "Timeout getting model config: %s [skipping connectivity checks]", ex.getMessage().c_str());
    } catch (InvalidConfigException& ex) {
        LOG(warning, "Invalid model config: %s [skipping connectivity checks]", ex.getMessage().c_str());
    } catch (ConfigRuntimeException& ex) {
        LOG(warning, "Runtime exception getting model config: %s [skipping connectivity checks]", ex.getMessage().c_str());
    }
}

void
ModelSubscriber::checkForUpdates() {
    if (! _modelHandle) {
        start(5ms);
    }
    if (_modelHandle && _subscriber.nextGenerationNow()) {
        if (auto newModel = _modelHandle->getConfig()) {
            LOG(config, "Sentinel got model info [version %s] for %zd hosts [config generation %" PRId64 "]",
                newModel->vespaVersion.c_str(), newModel->hosts.size(), _subscriber.getGeneration());
            _modelConfig = std::move(newModel);
        }
    }
}

}
