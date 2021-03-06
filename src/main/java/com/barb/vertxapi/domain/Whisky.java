package com.barb.vertxapi.domain;

public class Whisky {

  private int id;

  private String name;

  private String origin;

  public Whisky(String name, String origin) {
    this.name = name;
    this.origin = origin;
  }

  public int getId() {
    return id;
  }

  public void setId(final int id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public String getOrigin() {
    return origin;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setOrigin(String origin) {
    this.origin = origin;
  }
}
