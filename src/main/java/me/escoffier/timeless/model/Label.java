package me.escoffier.timeless.model;

/**
 * Represents a label.
 * <pre>
 *     {@code
 *     {
 *      "id": "string",
 *      "name": "string",
 *      "color": "string",
 *      "order": 0,
 *      "is_favorite": true
 *      }
*  }
 * </pre>
 * @param name the name
 * @param id the id
 */
public record Label(String name, String id) {

    public String getShortName() {
        // iterative to find the first uppercase
        int index = 0;
        boolean found = false;
        for (char c : name.toCharArray()) {
            if (Character.isUpperCase(c)) {
                found = true;
                break;
            }
            index = index + 1;
        }
        if (found) {
            return name.substring(index);
        } else {
            return name;
        }

    }

    @Override
    public String toString() {
        return name;
    }

}
