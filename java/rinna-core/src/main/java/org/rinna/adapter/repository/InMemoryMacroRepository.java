package org.rinna.adapter.repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.rinna.domain.model.macro.MacroDefinition;
import org.rinna.domain.model.macro.MacroExecution;
import org.rinna.domain.model.macro.ScheduledExecution;
import org.rinna.domain.model.macro.TriggerType;
import org.rinna.domain.repository.MacroRepository;

/**
 * In-memory implementation of the MacroRepository interface.
 */
public class InMemoryMacroRepository implements MacroRepository {
    private final Map<String, MacroDefinition> macros;
    private final Map<String, MacroExecution> executions;
    private final Map<String, ScheduledExecution> scheduledExecutions;

    /**
     * Default constructor.
     */
    public InMemoryMacroRepository() {
        this.macros = new ConcurrentHashMap<>();
        this.executions = new ConcurrentHashMap<>();
        this.scheduledExecutions = new ConcurrentHashMap<>();
    }

    @Override
    public MacroDefinition create(MacroDefinition macro) {
        if (macro.getId() == null) {
            macro.setId(UUID.randomUUID().toString());
        }
        
        macro.setCreatedAt(LocalDateTime.now());
        macro.setUpdatedAt(LocalDateTime.now());
        
        macros.put(macro.getId(), macro);
        return macro;
    }

    @Override
    public MacroDefinition findById(String id) {
        return macros.get(id);
    }

    @Override
    public MacroDefinition update(MacroDefinition macro) {
        if (macro.getId() == null || !macros.containsKey(macro.getId())) {
            throw new IllegalArgumentException("Cannot update non-existent macro");
        }
        
        macro.setUpdatedAt(LocalDateTime.now());
        macros.put(macro.getId(), macro);
        return macro;
    }

    @Override
    public boolean delete(String id) {
        if (macros.remove(id) != null) {
            // Also delete any scheduled executions for this macro
            deleteScheduledExecutionsByMacroId(id);
            return true;
        }
        return false;
    }

    @Override
    public List<MacroDefinition> findAll() {
        return new ArrayList<>(macros.values());
    }

    @Override
    public List<MacroDefinition> findByFilters(Map<String, String> filters) {
        if (filters == null || filters.isEmpty()) {
            return findAll();
        }
        
        return macros.values().stream()
                .filter(macro -> matchesFilters(macro, filters))
                .collect(Collectors.toList());
    }

    @Override
    public List<MacroDefinition> findByTriggerType(TriggerType triggerType) {
        if (triggerType == null) {
            return Collections.emptyList();
        }
        
        return macros.values().stream()
                .filter(macro -> macro.isEnabled() && 
                                 macro.getTrigger() != null && 
                                 macro.getTrigger().getType() == triggerType)
                .collect(Collectors.toList());
    }

    @Override
    public MacroExecution saveExecution(MacroExecution execution) {
        if (execution.getId() == null) {
            execution.setId(UUID.randomUUID().toString());
        }
        
        executions.put(execution.getId(), execution);
        
        // Also update the recent executions list in the macro
        if (execution.getMacroId() != null) {
            MacroDefinition macro = macros.get(execution.getMacroId());
            if (macro != null) {
                macro.addExecution(execution);
                // Limit the size of recent executions
                if (macro.getRecentExecutions().size() > 10) {
                    macro.getRecentExecutions().subList(0, macro.getRecentExecutions().size() - 10).clear();
                }
            }
        }
        
        return execution;
    }

    @Override
    public MacroExecution findExecutionById(String id) {
        return executions.get(id);
    }

    @Override
    public List<MacroExecution> findExecutionsByMacroId(String macroId, int limit) {
        if (macroId == null) {
            return Collections.emptyList();
        }
        
        return executions.values().stream()
                .filter(execution -> macroId.equals(execution.getMacroId()))
                .sorted(Comparator.comparing(MacroExecution::getStartTime).reversed())
                .limit(limit > 0 ? limit : Integer.MAX_VALUE)
                .collect(Collectors.toList());
    }

    @Override
    public ScheduledExecution saveScheduledExecution(ScheduledExecution scheduledExecution) {
        if (scheduledExecution.getId() == null) {
            scheduledExecution.setId(UUID.randomUUID().toString());
        }
        
        scheduledExecutions.put(scheduledExecution.getId(), scheduledExecution);
        return scheduledExecution;
    }

    @Override
    public List<ScheduledExecution> findAllScheduledExecutions() {
        return new ArrayList<>(scheduledExecutions.values());
    }

    @Override
    public List<ScheduledExecution> findScheduledExecutionsByMacroId(String macroId) {
        if (macroId == null) {
            return Collections.emptyList();
        }
        
        return scheduledExecutions.values().stream()
                .filter(execution -> macroId.equals(execution.getMacroId()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean deleteScheduledExecution(String id) {
        return scheduledExecutions.remove(id) != null;
    }

    @Override
    public int deleteScheduledExecutionsByMacroId(String macroId) {
        if (macroId == null) {
            return 0;
        }
        
        List<String> idsToRemove = scheduledExecutions.values().stream()
                .filter(execution -> macroId.equals(execution.getMacroId()))
                .map(ScheduledExecution::getId)
                .collect(Collectors.toList());
        
        idsToRemove.forEach(scheduledExecutions::remove);
        return idsToRemove.size();
    }

    /**
     * Checks if a macro matches the given filters.
     *
     * @param macro the macro to check
     * @param filters the filters to apply
     * @return true if the macro matches all filters
     */
    private boolean matchesFilters(MacroDefinition macro, Map<String, String> filters) {
        for (Map.Entry<String, String> filter : filters.entrySet()) {
            String key = filter.getKey();
            String value = filter.getValue();
            
            switch (key) {
                case "name":
                    if (!containsIgnoreCase(macro.getName(), value)) {
                        return false;
                    }
                    break;
                case "owner":
                    if (!Objects.equals(macro.getOwner(), value)) {
                        return false;
                    }
                    break;
                case "enabled":
                    if (macro.isEnabled() != Boolean.parseBoolean(value)) {
                        return false;
                    }
                    break;
                case "triggerType":
                    if (macro.getTrigger() == null || 
                        !macro.getTrigger().getType().name().equalsIgnoreCase(value)) {
                        return false;
                    }
                    break;
                default:
                    // Check in parameters
                    if (!Objects.equals(macro.getParameter(key), value)) {
                        return false;
                    }
                    break;
            }
        }
        
        return true;
    }

    /**
     * Checks if a string contains another string, ignoring case.
     *
     * @param str the string to check
     * @param subStr the substring to find
     * @return true if str contains subStr (ignoring case)
     */
    private boolean containsIgnoreCase(String str, String subStr) {
        if (str == null || subStr == null) {
            return false;
        }
        return str.toLowerCase().contains(subStr.toLowerCase());
    }
}