package confuzzion;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Stack;
import java.util.Timer;

import confuzzion.contract.Contract;
import confuzzion.contract.ContractTypeConfusion;
import confuzzion.mutations.BodyMutation;
import confuzzion.mutations.CallMethodMutation;
import confuzzion.mutations.Mutation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.G;
import soot.Scene;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class ConfuzzionMain {
    private Path resultFolder;

    private static final long MAIN_LOOP_ITERATIONS = -1; // no limit
    private static final int CONSTANTS_TRIES = 1;
    private static final long TIMEOUT = 10000L;
    private static final int STACK_LIMIT = Integer.MAX_VALUE;
    private static final boolean WITH_JVM = true;
    private static final long TIMER_TIMEOUT = 1000;
    private static final Logger logger = LoggerFactory.getLogger(ConfuzzionMain.class);

    public ConfuzzionMain(Path resultFolder) {
        this.resultFolder = resultFolder;
    }

    public static void main(String args[]) {
        final Options options = configParameters();
        CommandLineParser parser = new DefaultParser();

        Path resultFolder = Paths.get("confuzzionResults/");
        long main_loop_iterations = ConfuzzionMain.MAIN_LOOP_ITERATIONS;
        int constantsTries = ConfuzzionMain.CONSTANTS_TRIES;
        long timeout = ConfuzzionMain.TIMEOUT;
        int stackLimit = ConfuzzionMain.STACK_LIMIT;
        boolean withJVM = ConfuzzionMain.WITH_JVM;
        String javahome = System.getProperty("java.home");
        Path seedFile = null;
        String targets[];

        try {
            CommandLine line = parser.parse(options, args);

            if (line.hasOption("h")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("ConfuzzionMain [options]", options);
                return;
            }
            if (line.hasOption("o")) {
                resultFolder = Paths.get(line.getOptionValue("o"));
            }
            if (line.hasOption("i")) {
                main_loop_iterations = Long.parseLong(line.getOptionValue("i"));
            }
            if (line.hasOption("c")) {
                constantsTries = Integer.parseInt(line.getOptionValue("c"));
            }
            if (line.hasOption("timeout")) {
                timeout = Long.parseLong(line.getOptionValue("t"));
            }
            if (line.hasOption("l")) {
                stackLimit = Integer.parseInt(line.getOptionValue("l"));
            }
            withJVM = !line.hasOption("threads");
            if (line.hasOption("j")) {
                javahome = line.getOptionValue("j");
            }
            if (line.hasOption("s")) {
                seedFile = Paths.get(line.getOptionValue("s"));
            }
            if (line.hasOption("java-version")) {
                // soot.options.Options java_version corresponds to java_version + 1
                ConfuzzionOptions.v().java_version = Integer.parseInt(line.getOptionValue("java-version")) + 1;
            }
            if (line.hasOption("classes-limit")) {
                ConfuzzionOptions.v().class_number_limit = Integer.parseInt(line.getOptionValue("classes-limit"));
            }
            targets = line.getOptionValue("t").split(":");
            ConfuzzionOptions.v().use_jasmin_backend = line.hasOption("jasmin");
            ConfuzzionOptions.v().allow_unsafe_assignment = line.hasOption("unsafe-assignment");
            ConfuzzionOptions.v().fixed_number_of_classes = !line.hasOption("one-class");
            ConfuzzionOptions.v().use_uniform_distribution_for_methods = line.hasOption("uniform-methods-distribution");
            ConfuzzionOptions.v().quiet = line.hasOption("q");

            if (!Files.exists(resultFolder)) {
                Files.createDirectories(resultFolder);
            }

            ConfuzzionMain conf = new ConfuzzionMain(resultFolder);
            conf.startMutation(main_loop_iterations, timeout, stackLimit, withJVM, javahome, seedFile, constantsTries, targets);
        } catch (ParseException e) {
            logger.error("Options parsing failed", e);
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("confuzzion", options);
            System.exit(1);
        } catch (IOException e) {
            logger.error("Error", e);
            System.exit(1);
        }
    }

    private static Options configParameters() {
        final Option outputOption = Option.builder("o")
                .longOpt("output")
                .desc("Output directory")
                .hasArg(true)
                .argName("output")
                .required(false)
                .build();

        final Option iterationsOption = Option.builder("i")
                .longOpt("iterations")
                .desc("Main loop iterations / -1 no limit by default")
                .hasArg(true)
                .argName("iterations")
                .required(false)
                .build();

        final Option constantsTriesOption = Option.builder("c")
                .longOpt("constants-tries")
                .desc("Constants tries / 1 by default")
                .hasArg(true)
                .argName("tries")
                .required(false)
                .build();

        final Option timeoutOption = Option.builder()
                .longOpt("timeout")
                .desc("Timeout per program execution / 1000 ms by default")
                .hasArg(true)
                .argName("timeout")
                .required(false)
                .build();

        final Option runnerOption = Option.builder()
                .longOpt("threads")
                .desc("Use threads in spite of JVM to run programs")
                .hasArg(false)
                .required(false)
                .build();

        final Option jvmOption = Option.builder("j")
                .longOpt("jvm")
                .desc("JAVA_HOME for execution when not using --threads")
                .hasArg(true)
                .argName("jvm")
                .required(false)
                .build();

        final Option stackLimitOption = Option.builder("l")
                .longOpt("stack-limit")
                .desc("Mutations stack size limit / default no limit")
                .hasArg(true)
                .argName("stacklimit")
                .required(false)
                .build();

        final Option seedOption = Option.builder("s")
                .longOpt("seed")
                .desc("Seed folder where all jimple and class files are loaded before starting mutations")
                .hasArg(true)
                .argName("seed")
                .required(false)
                .build();

        final Option jasminOption = Option.builder()
                .longOpt("jasmin")
                .desc("Use Jasmin backend instead of ASM")
                .hasArg(false)
                .required(false)
                .build();

        final Option javaVersionOption = Option.builder()
                .longOpt("java-version")
                .desc("Force Java version of output bytecode to : 1-9")
                .hasArg(true)
                .argName("java-version")
                .required(false)
                .build();

        final Option unsafeAssignment = Option.builder()
                .longOpt("unsafe-assignment")
                .desc("Allow unsafe assignment in bytecode without checkcast")
                .hasArg(false)
                .required(false)
                .build();

        final Option classNumberOption = Option.builder()
                .longOpt("classes-limit")
                .desc("Max number of classes to create for the program / default 3")
                .hasArg(true)
                .argName("number-of-classes")
                .required(false)
                .build();

        final Option startWithOneClass = Option.builder()
                .longOpt("one-class")
                .desc("Start with only one class. Else immediately creates the max number of classes.")
                .hasArg(false)
                .required(false)
                .build();

        final Option uniformMethodsDistribution = Option.builder()
                .longOpt("uniform-methods-distribution")
                .desc("Select methods uniformly when applying a mutation. By default it tends to select methods with a high failure rate.")
                .hasArg(false)
                .required(false)
                .build();

        final Option quietOption = Option.builder("q")
                .longOpt("quiet")
                .desc("Do not print on standard output")
                .hasArg(false)
                .required(false)
                .build();

        final Option targetClassesOption = Option.builder("t")
                .longOpt("targets")
                .desc("List of target classes in Java form separated by ':'")
                .hasArg(true)
                .argName("target-classes")
                .required(true)
                .build();

        final Option helpOption = Option.builder("h")
                .longOpt("help")
                .desc("Print this message")
                .hasArg(false)
                .required(false)
                .build();

        final Options options = new Options();

        options.addOption(outputOption);
        options.addOption(iterationsOption);
        options.addOption(constantsTriesOption);
        options.addOption(timeoutOption);
        options.addOption(runnerOption);
        options.addOption(jvmOption);
        options.addOption(stackLimitOption);
        options.addOption(seedOption);
        options.addOption(jasminOption);
        options.addOption(javaVersionOption);
        options.addOption(unsafeAssignment);
        options.addOption(classNumberOption);
        options.addOption(startWithOneClass);
        options.addOption(uniformMethodsDistribution);
        options.addOption(quietOption);
        options.addOption(targetClassesOption);
        options.addOption(helpOption);

        return options;
    }

    public void startMutation(long mainloop_turn, long timeout, int stackLimit, boolean withJVM, String javahome, Path seedFolder, int constants_tries, String targets[]) {
        G.reset();
        soot.options.Options.v().set_weak_map_structures(true);
        Scene.v().loadBasicClasses();
        Scene.v().extendSootClassPath(Util.getJarPath());
        logger.info("Soot Class Path: {}", Scene.v().getSootClassPath());
        logger.info("Default java.home: {}", System.getProperty("java.home"));
        logger.info("Target java.home: {}", javahome);

        RandomGenerator rand = new RandomGenerator(targets);

        Program currentProg = null;
        if (seedFolder != null) {
            logger.info("Seed folder: {}", seedFolder);
            Scene.v().extendSootClassPath(seedFolder.toString());
            currentProg = new Program(rand, "Test", false);

            // Load all classes (.jimple/.class) in folder except the seed
            File folderFile = seedFolder.toFile();
            File[] listFiles = folderFile.listFiles();
            for (int i = listFiles.length - 1; i >= 0; i--) {
                File fileEntry = listFiles[i];
                if (fileEntry.isFile()) {
                    String filename = fileEntry.getName();

                    if (filename.endsWith(".jimple") || filename.endsWith(".class")) {
                        filename = filename.substring(0, filename.lastIndexOf("."));
                    } else {
                        continue;
                    }

                    logger.info("Loading class {}", filename);
                    Mutant mut = Mutant.loadClass(filename);
                    mut.fixClass();
                    currentProg.addMutant(mut);
                    if (logger.isDebugEnabled()) {
                        logger.debug(mut.toString());
                    }
                }
            }

            // Check all bodies for type confusion
            ArrayList<BodyMutation> bodyMutations = currentProg.addContractCheckAllBodies(new ContractTypeConfusion());
            Path tmpFolder = Paths.get(resultFolder.toAbsolutePath().toString(), "seed");
            try {
                try {
                    Files.createDirectories(tmpFolder);
                } catch(IOException e2) {
                    logger.error("Printing last program generated:\n{}", currentProg.toString(), e2);
                    return;
                }
                // Instantiation and launch
                if (withJVM) {
                    currentProg.genAndLaunchWithJVM(javahome, tmpFolder.toString(), timeout);
                } else { //with threads
                    currentProg.genAndLaunch(timeout);
                }
                currentProg.removeContractsChecks(bodyMutations);
            } catch(Throwable e) {
                Throwable cause = Util.getCause(e);
                if (cause instanceof ContractCheckException) {
                    logger.error("Seed already contains a contract check failure", cause);
                    currentProg.saveAsJimpleFiles(tmpFolder.toString());
                } else {
                    logger.error("Error while using seed for the first time", e);
                }
                return;
            }
        } else {
            currentProg = new Program(rand, "Test", true);
        }

        if (ConfuzzionOptions.v().fixed_number_of_classes) {
            // Generate new classes (with java.lang.Object as super type) until class_number_limit is reached
            for (int i = currentProg.getNumberOfMutants(); i < ConfuzzionOptions.v().class_number_limit; i++) {
                currentProg.genNewClass(true);
            }
        } //else: classes are added with AddClassMutation with any super type

        ArrayList<Contract> contracts = new ArrayList<Contract>(1);
        contracts.add(new ContractTypeConfusion());
        Stack<Mutation> mutationsStack = new Stack<Mutation>();

        // Refresh Status in command line each second
        Timer timer = new Timer();
        StatusScreen statusScreen = new StatusScreen();
        if (!ConfuzzionOptions.v().quiet) {
            timer.schedule(statusScreen, 0, TIMER_TIMEOUT);
        }
        final long startTime = System.nanoTime();

        for (long loop1 = 0; loop1 < mainloop_turn || mainloop_turn < 0; loop1++) {
            Mutation mutation = null;

            try {
                // Random mutation (program level | class level | method level)
                mutation = currentProg.randomMutation();
            } catch (MutationException e) {
                logger.warn("Exception while applying mutation", e);
                e.undoMutation();
                statusScreen.newMutation(e.getMutationClass(), Status.FAILED, 0);
                continue;
            } catch (Throwable e) {
                logger.error("Error while applying mutation", e);
                break;
            }

            logger.info("Mutation: {}", mutation.getClass().toString());
            if (logger.isDebugEnabled()) {
                logger.debug(currentProg.toString());
            }

            // Add contracts checks
            ArrayList<BodyMutation> contractsMutations =
                    currentProg.addContractsChecks(contracts, mutation);
            // Save current classes to unique folder
            Path folder = Paths.get(
                    resultFolder.toAbsolutePath().toString(),
                    loop1 + "-" + mutation.getClass().getSimpleName());
            Boolean keepFolder = false;
            int loop2 = 0;
            Status status = Status.NOTEXECUTED;
            try {
                // Instantiation and launch
                if (withJVM) {
                    try {
                        Files.createDirectories(folder);
                    } catch(IOException e2) {
                        logger.error("Printing last program generated:\n{}", currentProg.toString(), e2);
                        break;
                    }
                }

                for (loop2 = 0; loop2 < constants_tries; loop2++) {
                    try {
                        if (withJVM) {
                            currentProg.genAndLaunchWithJVM(javahome, folder.toString(), timeout);
                        } else { //with threads
                            currentProg.genAndLaunch(timeout);
                        }
                    } catch(Throwable e2) {
                        Throwable cause = Util.getCause(e2);
                        if (cause instanceof ContractCheckException || loop2 == constants_tries - 1) {
                            loop2++;
                            throw e2;
                        } else {
                            mutation.randomConstants();
                        }
                    }
                }

                // Remove contracts checks for next turn
                currentProg.removeContractsChecks(contractsMutations);
                // Add mutation to the stack
                mutationsStack.push(mutation);
                status = Status.SUCCESS;
            } catch(Throwable e) {
                logger.warn("Exception while executing program", e);
                Throwable cause = Util.getCause(e);
                if (cause instanceof ContractCheckException) {
                    keepFolder = true;
                    if (!withJVM) {
                        try {
                            Files.createDirectories(folder);
                            currentProg.saveAsClassFiles(folder.toString());
                        } catch(IOException e2) {
                            logger.error("Printing last program generated:\n{}", currentProg.toString(), e2);
                            break;
                        }
                    }
                    // Save current classes also as jimple files
                    currentProg.saveAsJimpleFiles(folder.toString());
                    // Save stats to stats.txt
                    String statsFile = Paths.get(folder.toString(), "stats.txt").toString();
                    String content = String.format("Found violation in %d ns\nStacked mutations: %d\n", System.nanoTime() - startTime, mutationsStack.size());
                    logger.info(content);
                    try {
                        Util.writeToFile(statsFile, content);
                    } catch (IOException e1) {
                        logger.error("Writing file {}", statsFile, e1);
                    }
                    status = Status.VIOLATES;
                } else if (cause instanceof InterruptedException) {
                    status = Status.INTERRUPTED;
                } else {
                    status = Status.CRASHED;
                }
                // Remove contracts checks
                currentProg.removeContractsChecks(contractsMutations);
                // Bad sample, revert mutation
                mutation.undo();
            } finally {
                // Update status screen
                statusScreen.newMutation(mutation.getClass(), status, loop2);
                if (mutation instanceof CallMethodMutation) {
                    CallMethodMutation cmm = (CallMethodMutation)mutation;
                    rand.addMethodCallStatus(cmm.getCalledMethod(), status == Status.SUCCESS || status == Status.VIOLATES);
                }
                if (withJVM && !keepFolder) {
                    // Remove folder
                    try {
                        Util.deleteDirectory(folder);
                    } catch(IOException e2) {
                        logger.error("Error while deleting directory {}", folder, e2);
                        break;
                    }
                }
            }

            if ((statusScreen.isStalled() && mutationsStack.size() > 0) || mutationsStack.size() >= stackLimit) {
                // Revert a random number of mutations
                int toRevert = rand.nextUint(mutationsStack.size());
                while(toRevert-- > 0) {
                    mutationsStack.pop().undo();
                }
                // Refresh stack size on status screen
                statusScreen.newStackSize(mutationsStack.size());
            }
        }
        // Stop automatic call to status.run()
        timer.cancel();
        // Print a last time the status screen
        statusScreen.run();
    }
}
