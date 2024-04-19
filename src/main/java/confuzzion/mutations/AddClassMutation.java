package confuzzion.mutations;

import confuzzion.Mutant;
import confuzzion.Program;
import confuzzion.RandomGenerator;
import soot.Scene;

public class AddClassMutation extends ProgramMutation {
    private Mutant addedClass;

    public AddClassMutation(RandomGenerator rand, Program program) {
        super(rand, program);

        addedClass = this.program.genNewClass(false);
    }

    @Override
    public void undo() {
        this.program.removeClass(addedClass);
        Scene.v().removeClass(addedClass.getSootClass());
    }

    @Override
    public void randomConstants() {
        // Nothing to do
    }
}
