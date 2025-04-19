/*
 * Example program demonstrating Ryorin-do v0.2 concepts
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.model;

/**
 * A simple example program that demonstrates the use of Ryorin-do v0.2 concepts
 * like CYNEFIN domains and work paradigms.
 */
public class RyorindoExample {
    
    public static void main(String[] args) {
        System.out.println("Ryorin-do v0.2 Example");
        System.out.println("=====================");
        
        // Demonstrate CYNEFIN domains
        System.out.println("\nCYNEFIN Domains:");
        System.out.println("--------------");
        for (CynefinDomain domain : CynefinDomain.values()) {
            System.out.println(domain.getName() + ": " + domain.getDescription());
            System.out.println("  Approach: " + domain.getRecommendedApproach());
            System.out.println("  Cognitive Load: " + domain.getSuggestedCognitiveLoadLevel() + "/10");
            
            System.out.print("  Appropriate Paradigms: ");
            String[] paradigms = domain.getAppropriateWorkParadigms();
            for (int i = 0; i < paradigms.length; i++) {
                System.out.print(paradigms[i]);
                if (i < paradigms.length - 1) {
                    System.out.print(", ");
                }
            }
            System.out.println("\n");
        }
        
        // Demonstrate work paradigms
        System.out.println("\nWork Paradigms:");
        System.out.println("--------------");
        for (WorkParadigm paradigm : WorkParadigm.values()) {
            System.out.println(paradigm.getName() + ": " + paradigm.getDescription());
            System.out.println("  Recommended CYNEFIN Domain: " + paradigm.getRecommendedDomain().getName());
            System.out.println("  Typical Time Horizon: " + paradigm.getTypicalTimeHorizon());
            System.out.println("  Cognitive Load Range: " + paradigm.getSuggestedCognitiveLoadRange() + "\n");
        }
        
        // Demonstrate the relationship between domains and paradigms
        System.out.println("\nDomain-Paradigm Relationships:");
        System.out.println("----------------------------");
        CynefinDomain domain = CynefinDomain.COMPLEX;
        System.out.println("For " + domain.getName() + " domain:");
        
        for (WorkParadigm paradigm : WorkParadigm.values()) {
            boolean suitable = paradigm.isSuitableFor(domain);
            System.out.println("  " + paradigm.getName() + ": " + 
                (suitable ? "Suitable" : "Not suitable"));
        }
    }
}