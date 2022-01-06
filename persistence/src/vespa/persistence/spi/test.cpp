// Copyright Yahoo. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#include "test.h"
#include "docentry.h"
#include <vespa/document/test/make_bucket_space.h>
#include <vespa/document/fieldvalue/document.h>

using document::BucketId;
using document::BucketSpace;
using document::test::makeBucketSpace;

namespace storage::spi::test {

Bucket
makeSpiBucket(BucketId bucketId)
{
    return Bucket(document::Bucket(makeBucketSpace(), bucketId));
}

std::unique_ptr<DocEntry>
cloneDocEntry(const DocEntry & e) {
    std::unique_ptr<DocEntry> ret;
    if (e.getDocument()) {
        ret = DocEntry::create(e.getTimestamp(), e.getFlags(),
                           std::make_unique<Document>(*e.getDocument()),
                           e.getDocumentSize());
    } else if (e.getDocumentId()) {
        ret = DocEntry::create(e.getTimestamp(), e.getFlags(), *e.getDocumentId());
    } else {
        ret = DocEntry::create(e.getTimestamp(), e.getFlags());
    }
    return ret;
}

bool
equal(const DocEntry & a, const DocEntry & b) {
    if (a.getTimestamp() != b.getTimestamp()) return false;
    if (a.getFlags() != b.getFlags()) return false;
    if (a.getDocumentSize() != b.getDocumentSize()) return false;

    if (a.getDocument()) {
        if (!b.getDocument()) return false;
        if (*a.getDocument() != *b.getDocument()) return false;
    } else {
        if (b.getDocument()) return false;
    }
    if (a.getDocumentId()) {
        if (!b.getDocumentId()) return false;
        if (*a.getDocumentId() != *b.getDocumentId()) return false;
    } else {
        if (b.getDocumentId()) return false;
    }

    return true;
}

}
