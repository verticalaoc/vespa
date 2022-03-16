package com.yahoo.searchdefinition.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * This class holds the extracted information after parsing a
 * "annotation" block, using simple data structures as far as
 * possible.  Do not put advanced logic here!
 * @author arnej27959
 **/
class ParsedAnnotation extends ParsedBlock {

    private ParsedStruct wrappedStruct = null;
    private final List<String> inherited = new ArrayList<>();
    private ParsedDocument ownedBy = null;

    ParsedAnnotation(String name) {
        super(name, "annotation");
    }

    public List<String> getInherited() { return List.copyOf(inherited); }
    public Optional<ParsedStruct> getStruct() { return Optional.ofNullable(wrappedStruct); }
    public ParsedDocument getOwnerDoc() { return ownedBy; }
    public String getOwnerName() { return ownedBy.name(); }

    void setStruct(ParsedStruct struct) { this.wrappedStruct = struct; }

    void inherit(String other) { inherited.add(other); }

    void tagOwner(ParsedDocument owner) {
        verifyThat(ownedBy == null, "already owned by", ownedBy);
        this.ownedBy = owner;
        getStruct().ifPresent(s -> s.tagOwner(owner));
    }
}
