/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.util;

import org.rinna.cli.domain.model.DefaultDomainWorkItem;
import org.rinna.cli.domain.model.DomainPriority;
import org.rinna.cli.domain.model.DomainWorkItem;
import org.rinna.cli.domain.model.DomainWorkItemType;
import org.rinna.cli.domain.model.DomainWorkflowState;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Utility class for mapping between CLI model classes and domain model classes.
 * This class provides methods to convert between different model representations.
 * It supports bidirectional conversion between CLI model classes and domain model
 * classes, including both the local domain model interfaces in the CLI module and
 * the actual domain interfaces in the core module.
 */
public final class ModelMapper {

    // Private constructor to prevent instantiation
    private ModelMapper() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Maps from CLI WorkflowState to domain WorkflowState string.
     *
     * @param state the CLI WorkflowState
     * @return the domain WorkflowState string
     */
    public static String toDomainWorkflowState(WorkflowState state) {
        return StateMapper.toCoreState(state);
    }

    /**
     * Maps from domain WorkflowState string to CLI WorkflowState.
     *
     * @param state the domain WorkflowState string
     * @return the CLI WorkflowState
     */
    public static WorkflowState toCliWorkflowState(String state) {
        return StateMapper.fromCoreState(state);
    }

    /**
     * Maps from CLI Priority to domain Priority string.
     *
     * @param priority the CLI Priority
     * @return the domain Priority string
     */
    public static String toDomainPriority(Priority priority) {
        return StateMapper.toCorePriority(priority);
    }

    /**
     * Maps from domain Priority string to CLI Priority.
     *
     * @param priority the domain Priority string
     * @return the CLI Priority
     */
    public static Priority toCliPriority(String priority) {
        return StateMapper.fromCorePriority(priority);
    }

    /**
     * Maps from CLI WorkItemType to domain WorkItemType string.
     * 
     * @param type the CLI WorkItemType
     * @return the domain WorkItemType string
     */
    public static String toDomainWorkItemType(WorkItemType type) {
        return StateMapper.toCoreType(type);
    }

    /**
     * Maps from domain WorkItemType string to CLI WorkItemType.
     *
     * @param type the domain WorkItemType string
     * @return the CLI WorkItemType
     */
    public static WorkItemType toCliWorkItemType(String type) {
        return StateMapper.fromCoreType(type);
    }
    
    /**
     * Converts a CLI WorkItem to a domain DomainWorkItem.
     *
     * @param cliItem the CLI WorkItem to convert
     * @return the domain DomainWorkItem
     */
    public static DomainWorkItem toDomainWorkItem(WorkItem cliItem) {
        if (cliItem == null) {
            return null;
        }
        
        DefaultDomainWorkItem domainItem = new DefaultDomainWorkItem();
        
        // Set basic properties
        if (cliItem.getId() != null) {
            try {
                domainItem.setId(UUID.fromString(cliItem.getId()));
            } catch (IllegalArgumentException e) {
                // If ID isn't a valid UUID, generate a new one
                domainItem.setId(UUID.randomUUID());
            }
        } else {
            domainItem.setId(UUID.randomUUID());
        }
        
        domainItem.setTitle(cliItem.getTitle());
        domainItem.setDescription(cliItem.getDescription());
        domainItem.setAssignee(cliItem.getAssignee());
        domainItem.setReporter(cliItem.getReporter());
        
        // Set type, priority, and state using the StateMapper
        if (cliItem.getType() != null) {
            domainItem.setType(StateMapper.toDomainType(cliItem.getType()));
        }
        
        if (cliItem.getPriority() != null) {
            domainItem.setPriority(StateMapper.toDomainPriority(cliItem.getPriority()));
        }
        
        if (cliItem.getStatus() != null) {
            domainItem.setState(StateMapper.toDomainState(cliItem.getStatus()));
        }
        
        // Handle timestamps
        if (cliItem.getCreated() != null) {
            domainItem.setCreatedAt(cliItem.getCreated());
        }
        
        if (cliItem.getUpdated() != null) {
            domainItem.setUpdatedAt(cliItem.getUpdated());
        }
        
        return domainItem;
    }
    
    /**
     * Converts a domain DomainWorkItem to a CLI WorkItem.
     *
     * @param domainItem the domain DomainWorkItem to convert
     * @return the CLI WorkItem
     */
    public static WorkItem toCliWorkItem(DomainWorkItem domainItem) {
        if (domainItem == null) {
            return null;
        }
        
        WorkItem cliItem = new WorkItem();
        
        // Set basic properties
        if (domainItem.getId() != null) {
            cliItem.setId(domainItem.getId().toString());
        }
        
        cliItem.setTitle(domainItem.getTitle());
        cliItem.setDescription(domainItem.getDescription());
        cliItem.setAssignee(domainItem.getAssignee());
        cliItem.setReporter(domainItem.getReporter());
        
        // Set type, priority, and state using the StateMapper
        if (domainItem.getType() != null) {
            cliItem.setType(StateMapper.fromDomainType(domainItem.getType()));
        }
        
        if (domainItem.getPriority() != null) {
            cliItem.setPriority(StateMapper.fromDomainPriority(domainItem.getPriority()));
        }
        
        if (domainItem.getState() != null) {
            cliItem.setStatus(StateMapper.fromDomainState(domainItem.getState()));
        }
        
        // Handle timestamps
        if (domainItem.getCreatedAt() != null) {
            cliItem.setCreated(LocalDateTime.ofInstant(domainItem.getCreatedAt(), ZoneId.systemDefault()));
        }
        
        if (domainItem.getUpdatedAt() != null) {
            cliItem.setUpdated(LocalDateTime.ofInstant(domainItem.getUpdatedAt(), ZoneId.systemDefault()));
        }
        
        return cliItem;
    }
    
    /**
     * Converts a core module WorkItem to a CLI WorkItem.
     * This method supports the core domain model interfaces from the rinna-core module.
     *
     * @param coreItem the core module WorkItem to convert
     * @return the CLI WorkItem
     */
    public static WorkItem toCliWorkItemFromCore(Object coreItem) {
        if (coreItem == null) {
            return null;
        }
        
        try {
            // Use reflection to access properties from core WorkItem
            Class<?> coreClass = coreItem.getClass();
            WorkItem cliItem = new WorkItem();
            
            // Get ID
            try {
                Object id = invokeGetter(coreItem, "getId");
                if (id != null) {
                    cliItem.setId(id.toString());
                }
            } catch (Exception e) {
                // If getId fails, try alternate getter
                Object id = invokeGetter(coreItem, "id");
                if (id != null) {
                    cliItem.setId(id.toString());
                }
            }
            
            // Get title
            String title = (String) invokeGetter(coreItem, "getTitle");
            cliItem.setTitle(title);
            
            // Get description
            String description = (String) invokeGetter(coreItem, "getDescription");
            cliItem.setDescription(description);
            
            // Get assignee
            String assignee = (String) invokeGetter(coreItem, "getAssignee");
            cliItem.setAssignee(assignee);
            
            // Get reporter
            String reporter = (String) invokeGetter(coreItem, "getReporter");
            cliItem.setReporter(reporter);
            
            // Get type
            Object typeObj = invokeGetter(coreItem, "getType");
            if (typeObj != null) {
                String typeStr = typeObj.toString();
                cliItem.setType(StateMapper.fromCoreType(typeStr));
            }
            
            // Get priority
            Object priorityObj = invokeGetter(coreItem, "getPriority");
            if (priorityObj != null) {
                String priorityStr = priorityObj.toString();
                cliItem.setPriority(StateMapper.fromCorePriority(priorityStr));
            }
            
            // Get state
            Object stateObj = invokeGetter(coreItem, "getState");
            if (stateObj == null) {
                // Try alternate getter
                stateObj = invokeGetter(coreItem, "getStatus");
            }
            if (stateObj != null) {
                String stateStr = stateObj.toString();
                cliItem.setStatus(StateMapper.fromCoreState(stateStr));
            }
            
            // Get creation timestamp
            Object createdObj = invokeGetter(coreItem, "getCreatedAt");
            if (createdObj instanceof Instant) {
                Instant createdInstant = (Instant) createdObj;
                cliItem.setCreated(LocalDateTime.ofInstant(createdInstant, ZoneId.systemDefault()));
            }
            
            // Get update timestamp
            Object updatedObj = invokeGetter(coreItem, "getUpdatedAt");
            if (updatedObj instanceof Instant) {
                Instant updatedInstant = (Instant) updatedObj;
                cliItem.setUpdated(LocalDateTime.ofInstant(updatedInstant, ZoneId.systemDefault()));
            }
            
            return cliItem;
            
        } catch (Exception e) {
            // Fallback to default string conversion if reflection fails
            WorkItem cliItem = new WorkItem();
            cliItem.setTitle("Conversion error: " + e.getMessage());
            cliItem.setDescription("Failed to convert core work item: " + coreItem);
            return cliItem;
        }
    }
    
    /**
     * Converts a CLI WorkItem to a core module WorkItem.
     * This method supports the core domain model interfaces from the rinna-core module.
     *
     * @param cliItem the CLI WorkItem to convert
     * @param coreClass the core module WorkItem class to instantiate
     * @return the core module WorkItem
     */
    public static Object toCoreWorkItem(WorkItem cliItem, Class<?> coreClass) {
        if (cliItem == null || coreClass == null) {
            return null;
        }
        
        try {
            // Convert to domain model first
            DomainWorkItem domainItem = toDomainWorkItem(cliItem);
            
            // Create core model instance
            Object coreItem = coreClass.getDeclaredConstructor().newInstance();
            
            // Set ID
            if (domainItem.getId() != null) {
                invokeSetter(coreItem, "setId", domainItem.getId());
            }
            
            // Set title
            invokeSetter(coreItem, "setTitle", domainItem.getTitle());
            
            // Set description
            invokeSetter(coreItem, "setDescription", domainItem.getDescription());
            
            // Set assignee
            invokeSetter(coreItem, "setAssignee", domainItem.getAssignee());
            
            // Set reporter
            invokeSetter(coreItem, "setReporter", domainItem.getReporter());
            
            // Set type
            if (domainItem.getType() != null) {
                String typeStr = domainItem.getType().name();
                // Attempt to map to core enum if available
                try {
                    Class<?> enumClass = getCoreEnumClass(coreClass, "WorkItemType");
                    if (enumClass != null) {
                        Object enumValue = Enum.valueOf((Class<Enum>)enumClass, typeStr);
                        invokeSetter(coreItem, "setType", enumValue);
                    } else {
                        // Fallback to string
                        invokeSetter(coreItem, "setType", typeStr);
                    }
                } catch (Exception e) {
                    // If enum conversion fails, use string value
                    invokeSetter(coreItem, "setType", typeStr);
                }
            }
            
            // Set priority
            if (domainItem.getPriority() != null) {
                String priorityStr = domainItem.getPriority().name();
                // Attempt to map to core enum if available
                try {
                    Class<?> enumClass = getCoreEnumClass(coreClass, "Priority");
                    if (enumClass != null) {
                        Object enumValue = Enum.valueOf((Class<Enum>)enumClass, priorityStr);
                        invokeSetter(coreItem, "setPriority", enumValue);
                    } else {
                        // Fallback to string
                        invokeSetter(coreItem, "setPriority", priorityStr);
                    }
                } catch (Exception e) {
                    // If enum conversion fails, use string value
                    invokeSetter(coreItem, "setPriority", priorityStr);
                }
            }
            
            // Set state
            if (domainItem.getState() != null) {
                String stateStr = domainItem.getState().name();
                // Attempt to map to core enum if available
                try {
                    Class<?> enumClass = getCoreEnumClass(coreClass, "WorkflowState");
                    if (enumClass != null) {
                        Object enumValue = Enum.valueOf((Class<Enum>)enumClass, stateStr);
                        // Try both setter names
                        try {
                            invokeSetter(coreItem, "setState", enumValue);
                        } catch (Exception e) {
                            invokeSetter(coreItem, "setStatus", enumValue);
                        }
                    } else {
                        // Fallback to string
                        try {
                            invokeSetter(coreItem, "setState", stateStr);
                        } catch (Exception e) {
                            invokeSetter(coreItem, "setStatus", stateStr);
                        }
                    }
                } catch (Exception e) {
                    // If enum conversion fails, use string value
                    try {
                        invokeSetter(coreItem, "setState", stateStr);
                    } catch (Exception ex) {
                        invokeSetter(coreItem, "setStatus", stateStr);
                    }
                }
            }
            
            // Set timestamps
            if (domainItem.getCreatedAt() != null) {
                invokeSetter(coreItem, "setCreatedAt", domainItem.getCreatedAt());
            }
            
            if (domainItem.getUpdatedAt() != null) {
                invokeSetter(coreItem, "setUpdatedAt", domainItem.getUpdatedAt());
            }
            
            return coreItem;
            
        } catch (Exception e) {
            // If conversion fails, return null or throw exception
            return null;
        }
    }
    
    /**
     * Converts a list of CLI WorkItems to a list of domain DomainWorkItems.
     *
     * @param cliItems the list of CLI WorkItems to convert
     * @return the list of domain DomainWorkItems
     */
    public static List<DomainWorkItem> toDomainWorkItems(List<WorkItem> cliItems) {
        if (cliItems == null) {
            return null;
        }
        
        return cliItems.stream()
                .map(ModelMapper::toDomainWorkItem)
                .collect(Collectors.toList());
    }
    
    /**
     * Converts a list of domain DomainWorkItems to a list of CLI WorkItems.
     *
     * @param domainItems the list of domain DomainWorkItems to convert
     * @return the list of CLI WorkItems
     */
    public static List<WorkItem> toCliWorkItems(List<DomainWorkItem> domainItems) {
        if (domainItems == null) {
            return null;
        }
        
        return domainItems.stream()
                .map(ModelMapper::toCliWorkItem)
                .collect(Collectors.toList());
    }
    
    /**
     * Converts a list of core module WorkItems to a list of CLI WorkItems.
     *
     * @param coreItems the list of core module WorkItems to convert
     * @return the list of CLI WorkItems
     */
    public static List<WorkItem> toCliWorkItemsFromCore(List<?> coreItems) {
        if (coreItems == null) {
            return null;
        }
        
        return coreItems.stream()
                .map(ModelMapper::toCliWorkItemFromCore)
                .collect(Collectors.toList());
    }
    
    /**
     * Converts a CLI UUID string to a UUID object.
     * If the string is not a valid UUID, a new random UUID is generated.
     *
     * @param id the CLI ID string
     * @return the UUID object
     */
    public static UUID toUUID(String id) {
        if (id == null || id.isEmpty()) {
            return UUID.randomUUID();
        }
        
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            // If ID isn't a valid UUID, generate a new one
            return UUID.randomUUID();
        }
    }
    
    // Helper methods for reflection
    
    private static Object invokeGetter(Object obj, String methodName) throws Exception {
        try {
            return obj.getClass().getMethod(methodName).invoke(obj);
        } catch (NoSuchMethodException e) {
            // Try without 'get' prefix if method not found
            if (methodName.startsWith("get")) {
                String fieldName = methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
                try {
                    return obj.getClass().getMethod(fieldName).invoke(obj);
                } catch (NoSuchMethodException ex) {
                    // Field access as last resort
                    try {
                        return obj.getClass().getField(fieldName).get(obj);
                    } catch (NoSuchFieldException exc) {
                        return null;
                    }
                }
            }
            return null;
        }
    }
    
    private static void invokeSetter(Object obj, String methodName, Object value) throws Exception {
        try {
            // Find the appropriate setter method
            for (java.lang.reflect.Method method : obj.getClass().getMethods()) {
                if (method.getName().equals(methodName) && method.getParameterCount() == 1) {
                    method.invoke(obj, value);
                    return;
                }
            }
            throw new NoSuchMethodException(methodName);
        } catch (NoSuchMethodException e) {
            // Try without 'set' prefix if method not found
            if (methodName.startsWith("set")) {
                String fieldName = methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
                try {
                    obj.getClass().getField(fieldName).set(obj, value);
                } catch (NoSuchFieldException ex) {
                    // Ignore - field not found
                }
            }
        }
    }
    
    private static Class<?> getCoreEnumClass(Class<?> contextClass, String enumName) {
        try {
            // Try in the same package
            String packageName = contextClass.getPackage().getName();
            return Class.forName(packageName + "." + enumName);
        } catch (ClassNotFoundException e1) {
            try {
                // Try in parent package
                String packageName = contextClass.getPackage().getName();
                int lastDot = packageName.lastIndexOf('.');
                if (lastDot > 0) {
                    packageName = packageName.substring(0, lastDot);
                }
                return Class.forName(packageName + "." + enumName);
            } catch (ClassNotFoundException e2) {
                try {
                    // Try in model subpackage
                    String packageName = contextClass.getPackage().getName();
                    return Class.forName(packageName + ".model." + enumName);
                } catch (ClassNotFoundException e3) {
                    // Class not found
                    return null;
                }
            }
        }
    }
}