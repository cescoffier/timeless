package me.escoffier.timeless.model;


import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a project.
 *
 * <pre>
 *     {@code
 *     {
 *      "id": "string",
 *      "parent_id": "string",
 *      "order": 0,
 *      "color": "string",
 *      "name": "string",
 *      "is_shared": true,
 *      "is_favorite": true,
 *      "is_inbox_project": true,
 *      "is_team_inbox": true,
 *      "url": "string",
 *      "view_style": "string",
 *      "description": "string"
 *      }
 *     }
 * </pre>
 *
 * @param id the id
 * @param name the name
 * @param parent the parent id
 */
public record Project(String id, String name,
                      @JsonProperty("parent_id") String parent) {

}
