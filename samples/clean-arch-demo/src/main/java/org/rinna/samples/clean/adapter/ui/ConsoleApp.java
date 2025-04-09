package org.rinna.samples.clean.adapter.ui;

import org.rinna.samples.clean.adapter.persistence.InMemoryTaskRepository;
import org.rinna.samples.clean.domain.entity.Task;
import org.rinna.samples.clean.domain.usecase.TaskRepository;
import org.rinna.samples.clean.domain.usecase.TaskService;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Console UI implementation for the task management app
 */
public class ConsoleApp {
    private final TaskService taskService;
    private final Scanner scanner;

    public ConsoleApp(TaskService taskService) {
        this.taskService = taskService;
        this.scanner = new Scanner(System.in);
    }

    public static void main(String[] args) {
        TaskRepository repository = new InMemoryTaskRepository();
        TaskService service = new TaskService(repository);
        ConsoleApp app = new ConsoleApp(service);
        
        // Add sample data
        service.createTask("Learn Clean Architecture", "Study the principles of Clean Architecture");
        service.createTask("Implement Rinna App", "Create a workflow management application");
        
        app.run();
    }

    public void run() {
        boolean running = true;
        while (running) {
            displayMenu();
            int choice = readIntInput();
            
            switch (choice) {
                case 1:
                    listTasks();
                    break;
                case 2:
                    addTask();
                    break;
                case 3:
                    completeTask();
                    break;
                case 4:
                    deleteTask();
                    break;
                case 5:
                    running = false;
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
        
        System.out.println("Goodbye!");
    }
    
    private void displayMenu() {
        System.out.println("\n--- Task Manager ---");
        System.out.println("1. List all tasks");
        System.out.println("2. Add new task");
        System.out.println("3. Mark task as completed");
        System.out.println("4. Delete task");
        System.out.println("5. Exit");
        System.out.print("Enter your choice: ");
    }
    
    private void listTasks() {
        List<Task> tasks = taskService.getAllTasks();
        
        if (tasks.isEmpty()) {
            System.out.println("No tasks found.");
            return;
        }
        
        System.out.println("\nTasks:");
        for (Task task : tasks) {
            System.out.printf("%s - %s [%s]%n", 
                    task.getId(), 
                    task.getTitle(),
                    task.isCompleted() ? "Completed" : "Pending");
        }
    }
    
    private void addTask() {
        System.out.print("Enter task title: ");
        String title = scanner.nextLine();
        
        System.out.print("Enter task description: ");
        String description = scanner.nextLine();
        
        Task task = taskService.createTask(title, description);
        System.out.println("Task created with ID: " + task.getId());
    }
    
    private void completeTask() {
        System.out.print("Enter task ID to mark as completed: ");
        String id = scanner.nextLine();
        
        Optional<Task> result = taskService.completeTask(id);
        if (result.isPresent()) {
            System.out.println("Task marked as completed.");
        } else {
            System.out.println("Task not found.");
        }
    }
    
    private void deleteTask() {
        System.out.print("Enter task ID to delete: ");
        String id = scanner.nextLine();
        
        taskService.deleteTask(id);
        System.out.println("Task deleted (if it existed).");
    }
    
    private int readIntInput() {
        try {
            return Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
