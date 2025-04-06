/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli;

import org.rinna.cli.command.AddCommand;
import org.rinna.cli.command.ListCommand;
import org.rinna.cli.command.ServerCommand;
import org.rinna.cli.command.UpdateCommand;
import org.rinna.cli.command.ViewCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/**
 * Main entry point for the Rinna CLI.
 */
@Command(
    name = "rin-cli",
    description = "Command line interface for Rinna workflow management system",
    mixinStandardHelpOptions = true,
    version = "Rinna CLI 1.3.6",
    subcommands = {
        AddCommand.class,
        ListCommand.class,
        ViewCommand.class,
        UpdateCommand.class,
        ServerCommand.class
    }
)
public class RinnaCli implements Callable<Integer> {
    
    @Override
    public Integer call() {
        // Default behavior is to show help
        CommandLine.usage(this, System.out);
        return 0;
    }
    
    /**
     * Main method to start the CLI.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new RinnaCli()).execute(args);
        System.exit(exitCode);
    }
}
