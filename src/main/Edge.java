package main; /**
 * Created by atuladhar on 6/27/17.
 */
import processing.core.PApplet;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;

public class Edge implements Comparable<Edge> {
  PApplet parent;
  String start, end;
  int startPixel, endPixel;
  int sx, sy, ex, ey;
  Integer pixelDistance;
  Float distance;
  String direction;
  List<Integer> path;

  Node source, target;

  public Edge(PApplet p, int s, int e, int pixelDist) {
    this(p, s, e);
    this.pixelDistance = pixelDist;
    this.distance = calculateDistance(pixelDist);
  }

  public Edge(PApplet p, Node s, Node t, int pixelDist, List<Integer> path) {
    this.parent = p;
    this.source = s;
    this.target = t;
    this.distance = calculateDistance(pixelDist);
    this.pixelDistance = pixelDist;
    this.direction = s.directionTo(t);
    this.path = new LinkedList<Integer>(path);
  }

  public Edge(PApplet p, Node s, Node t, int pixelDist){
    this(p, s, t, pixelDist, new LinkedList<Integer>());
    this.parent = p;
  }

  public Edge(PApplet p, int s, int e) {
    this.parent = p;
    this.startPixel = s;
    this.endPixel = e;
    this.sx = s % p.width;
    this.sy = s / p.width;
    this.ex = e % p.width;
    this.ey = e / p.width;
    this.pixelDistance = Integer.MIN_VALUE;
    this.distance = Float.MIN_VALUE;
  }

  private Float calculateDistance(int pixel) {
    Float dist = ((float)12/200) * pixel;
    BigDecimal bdDist = new BigDecimal(dist.toString());
    bdDist = bdDist.setScale(2, BigDecimal.ROUND_HALF_UP);
    return bdDist.floatValue();
  }

  public Node getTarget() {
    return this.target;
  }



  @Override
  public boolean equals(Object other){
    if (!(other instanceof Edge)) return false;

    Edge e = (Edge) other;

    return (this.source.equals(e.source)
        && this.target.equals(e.target)
        && this.direction.equals(e.direction)
        && this.pixelDistance.equals(e.pixelDistance));
  }

  @Override
  public int hashCode() {
    int hash = pixelDistance.hashCode();
    hash = hash * 31 + source.hashCode();
    hash = hash * 31 + direction.hashCode();
    hash = hash * 31 + target.hashCode();
    return hash;
  }

  public int compareTo(Edge compareEdge) {

      int compareQuantity = compareEdge.pixelDistance;

      //ascending order
      return this.pixelDistance - compareQuantity;

      //descending order
      //return compareQuantity - this.pixelDistance;
  }
}