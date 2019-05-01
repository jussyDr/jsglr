package org.spoofax.jsglr2.measure;

import java.util.ArrayList;
import java.util.List;

import org.metaborg.parsetable.IState;
import org.metaborg.parsetable.actions.IGoto;
import org.metaborg.parsetable.characterclasses.ICharacterClass;
import org.metaborg.sdf2table.parsetable.query.ActionsForCharacterDisjointSorted;
import org.metaborg.sdf2table.parsetable.query.ActionsForRange;
import org.metaborg.sdf2table.parsetable.query.ActionsPerCharacterClass;
import org.spoofax.jsglr2.states.StateFactory;

public class MeasureStateFactory extends StateFactory {

    public int statesCount = 0;
    public int statesDisjointSortableCharacterClassesCount = 0;

    public int gotosCount = 0;
    public int actionGroupsCount = 0;
    public int actionDisjointSortedRangesCount = 0;
    public int actionsCount = 0;

    public int gotosPerStateMax = 0;
    public int actionGroupsPerStateMax = 0;
    public int actionDisjointSortedRangesPerStateMax = 0;
    public int actionsPerStateMax = 0;
    public int actionsPerGroupMax = 0;
    public int actionsPerDisjointSortedRangeMax = 0;

    @Override public IState from(int stateNumber, IGoto[] gotos,
        ActionsPerCharacterClass[] actionsPerCharacterClasses) {
        statesCount++;

        gotosCount += gotos.length;
        actionGroupsCount += actionsPerCharacterClasses.length;

        int actionsCount = 0;

        List<ICharacterClass> characterClasses = new ArrayList<>(actionsPerCharacterClasses.length);

        for(ActionsPerCharacterClass actionsPerCharacterClass : actionsPerCharacterClasses) {
            actionsCount += actionsPerCharacterClass.actions.size();

            actionsPerGroupMax = Math.max(actionsPerGroupMax, actionsPerCharacterClass.actions.size());

            characterClasses.add(actionsPerCharacterClass.characterClass);
        }

        statesDisjointSortableCharacterClassesCount += ICharacterClass.disjointSortable(characterClasses) ? 1 : 0;

        ActionsForRange[] actionsForSortedDisjointRanges =
            ActionsForCharacterDisjointSorted.toDisjointSortedRanges(actionsPerCharacterClasses);

        actionDisjointSortedRangesCount += actionsForSortedDisjointRanges.length;

        for(ActionsForRange actionsForSortedDisjointRange : actionsForSortedDisjointRanges) {
            actionsPerDisjointSortedRangeMax =
                Math.max(actionsPerDisjointSortedRangeMax, actionsForSortedDisjointRange.actions.length);
        }

        actionsCount += actionsCount;

        gotosPerStateMax = Math.max(gotosPerStateMax, gotos.length);
        actionGroupsPerStateMax = Math.max(actionGroupsPerStateMax, actionsPerCharacterClasses.length);
        actionDisjointSortedRangesPerStateMax =
            Math.max(actionDisjointSortedRangesPerStateMax, actionsForSortedDisjointRanges.length);
        actionsPerStateMax = Math.max(actionsPerStateMax, actionsCount);

        return super.from(stateNumber, gotos, actionsPerCharacterClasses);
    }

}
