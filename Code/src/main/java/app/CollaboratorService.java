package app;

import java.util.*;

public class CollaboratorService {

    public void showOverloaded(List<Collaborator> collaborators) {

        System.out.println("Overloaded Collaborators:");

        for (Collaborator c : collaborators) {
            if (c.isOverloaded()) {
                System.out.println("- " + c.getName());
            }
        }
    }
}
