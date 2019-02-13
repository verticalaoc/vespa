// Copyright 2019 Oath Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
#pragma once

#include "primitivedatatype.h"

namespace document {

/*
 * This class describes a tensor type.
 */
class TensorDataType : public PrimitiveDataType {
public:
    TensorDataType();
    
    std::unique_ptr<FieldValue> createFieldValue() const override;
    TensorDataType* clone() const override;
    void print(std::ostream&, bool verbose, const std::string& indent) const override;
    static std::unique_ptr<const TensorDataType> fromSpec(const vespalib::string &spec);
    
    DECLARE_IDENTIFIABLE_ABSTRACT(TensorDataType);
};

}
