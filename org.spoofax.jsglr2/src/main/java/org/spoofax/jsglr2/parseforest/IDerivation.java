package org.spoofax.jsglr2.parseforest;

import org.metaborg.parsetable.productions.IProduction;
import org.metaborg.parsetable.productions.ProductionType;

public interface IDerivation<ParseForest extends IParseForest> {

    IProduction production();

    ProductionType productionType();

    ParseForest[] parseForests();

    default String descriptor() {
        return production().descriptor();
    }

}
