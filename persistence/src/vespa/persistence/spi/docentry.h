// Copyright Yahoo. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
/**
 * \class storage::spi::DocEntry
 * \ingroup spi
 *
 * \brief Represents a document with metadata stored.
 *
 * To do merge, all SPI implementations need some common metadata. To do iterate
 * efficiently, we also want options to only return metadata or similar. Thus
 * we need a class to contain all generic parts stored by all SPI
 * implementations.
 */

#pragma once

#include <persistence/spi/types.h>

namespace storage::spi {

enum DocumentMetaFlags {
    NONE             = 0x0,
    REMOVE_ENTRY     = 0x1
};

class DocEntry {
public:
    using SizeType = uint32_t;
    using UP = std::unique_ptr<DocEntry>;
    using SP = std::shared_ptr<DocEntry>;

    DocEntry(Timestamp t, int metaFlags) : DocEntry(t, metaFlags, 0) { }
    DocEntry(const DocEntry &) = delete;
    DocEntry & operator=(const DocEntry &) = delete;
    DocEntry(DocEntry &&) = delete;
    DocEntry & operator=(DocEntry &&) = delete;
    virtual ~DocEntry();
    bool isRemove() const { return (_metaFlags & REMOVE_ENTRY); }
    Timestamp getTimestamp() const { return _timestamp; }
    int getFlags() const { return _metaFlags; }
    /**
     * @return In-memory size of this doc entry, including document instance.
     *     In essence: serialized size of document + sizeof(DocEntry).
     */
    SizeType getSize() const { return _size + getOwnSize() ; }
    virtual SizeType getOwnSize() const { return sizeof(DocEntry); }
    /**
     * If entry contains a document, returns its serialized size.
     * If entry contains a document id, returns the serialized size of
     * the id alone.
     * Otherwise (i.e. metadata only), returns zero.
     */
    SizeType getDocumentSize() const { return _size; }

    virtual vespalib::string toString() const;
    virtual const Document* getDocument() const { return nullptr; }
    virtual const DocumentId* getDocumentId() const { return nullptr; }
    virtual DocumentUP releaseDocument();
    static UP create(Timestamp t, int metaFlags);
    static UP create(Timestamp t, int metaFlags, const DocumentId &docId);
    static UP create(Timestamp t, int metaFlags, DocumentUP doc);
    static UP create(Timestamp t, int metaFlags, DocumentUP doc, SizeType serializedDocumentSize);
protected:
    DocEntry(Timestamp t, int metaFlags, SizeType size)
        : _timestamp(t),
          _metaFlags(metaFlags),
          _size(size)
    {}
private:
    Timestamp    _timestamp;
    int          _metaFlags;
    SizeType     _size;
};

std::ostream & operator << (std::ostream & os, const DocEntry & r);

}
