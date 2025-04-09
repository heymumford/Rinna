package org.rinna.cli;

import org.rinna.cli.command.AdminCommand;
import java.util.concurrent.Callable;

/**
 * Simplified version of RinnaCli for testing.
 */
public class RinnaCli implements Callable<Integer> {
    
    @Override
    public Integer call() {
        showHelp();
        return 0;
    }
    
    private void showHelp() {
        System.out.println("Rinna CLI - Command line interface for Rinna workflow management system");
        System.out.println("Version: 1.8.1");
        System.out.println();
        System.out.println("Usage: rin-cli [command] [options]");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  view        View details of a work item");
        System.out.println("  list        List work items");
        System.out.println("  admin       Administrative operations");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -h, --help     Show this help message and exit");
        System.out.println("  -v, --version  Show version information and exit");
    }
    
    /**
     * Handles the admin command.
     *
     * @param subargs command arguments
     * @return exit code
     */
    private static int handleAdminCommand(String[] subargs) {
        AdminCommand adminCmd = new AdminCommand();
        
        if (subargs.length > 0) {
            adminCmd.setSubcommand(subargs[0]);
            
            if (subargs.length > 1) {
                String[] restArgs = new String[subargs.length - 1];
                System.arraycopy(subargs, 1, restArgs, 0, subargs.length - 1);
                adminCmd.setArgs(restArgs);
            }
        }
        
        return adminCmd.call();
    }
    
    /**
     * Test method for running the admin command.
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            new RinnaCli().call();
            return;
        }
        
        String command = args[0];
        String[] subargs = new String[args.length - 1];
        System.arraycopy(args, 1, subargs, 0, args.length - 1);
        
        int exitCode;
        
        if ("admin".equals(command)) {
            exitCode = handleAdminCommand(subargs);
        } else if ("-h".equals(command) || "--help".equals(command)) {
            new RinnaCli().showHelp();
            exitCode = 0;
        } else if ("-v".equals(command) || "--version".equals(command)) {
            System.out.println("Rinna CLI version 1.8.1");
            exitCode = 0;
        } else {
            System.out.println("Unknown command: " + command);
            new RinnaCli().showHelp();
            exitCode = 1;
        }
        
        System.exit(exitCode);
    }
}