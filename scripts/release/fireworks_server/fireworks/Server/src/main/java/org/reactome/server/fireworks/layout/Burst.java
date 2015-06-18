package org.reactome.server.fireworks.layout;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.annotation.Generated;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
        "name",
        "centerX",
        "centerY",
        "startAngle",
        "direction"
})
public class Burst {

    private Long dbId;
    private String name;

    private Double centerX;
    private Double centerY;
    private Double startAngle;
    private Direction direction;

    public Burst(Long dbId, String name, Double centerX, Double centerY, Double startAngle, Direction direction) {
        this(name, centerX, centerY, startAngle, direction);
        this.dbId = dbId;
    }

    public Burst(@JsonProperty("name") String name,
                 @JsonProperty("centerX") double centerX,
                 @JsonProperty("centerY") double centerY,
                 @JsonProperty("startAngle") double startAngle,
                 @JsonProperty("direction") Direction direction) {
        this.name = name;
        this.centerX = centerX;
        this.centerY = centerY;
        this.startAngle = startAngle;
        this.direction = direction;
    }

    @JsonIgnore
    public Long getDbId() {
        return dbId;
    }

    public void setDbId(Long dbId) {
        this.dbId = dbId;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("centerX")
    public Double getCenterX() {
        return centerX;
    }

    @JsonProperty("centerX")
    public void setCenterX(Double centerX) {
        this.centerX = centerX;
    }

    @JsonProperty("centerY")
    public Double getCenterY() {
        return centerY;
    }

    @JsonProperty("centerY")
    public void setCenterY(Double centerY) {
        this.centerY = centerY;
    }

    @JsonProperty("startAngle")
    public Double getStartAngle() {
        return startAngle;
    }

    @JsonProperty("startAngle")
    public void setStartAngle(Double startAngle) {
        this.startAngle = startAngle;
    }


    @JsonIgnore
    public Double getEndAngle() {
        return startAngle + (direction.equals(Direction.CLOCKWISE) ? 360 : -360);
    }

    @JsonProperty("direction")
    public Direction getDirection() {
        return direction;
    }

    @JsonProperty("direction")
    public void setDirection(String direction) {
        this.direction = Direction.getDirection(direction);
    }

    @Override
    public String toString() {
        return "Burst{" +
                "centerX=" + centerX +
                ", centerY=" + centerY +
                ", startAngle=" + startAngle +
                ", endAngle=" + getEndAngle() +
                ", direction=" + direction +
                '}';
    }
}
