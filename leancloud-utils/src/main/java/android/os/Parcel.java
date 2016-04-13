package android.os;

public interface Parcel {
  public void writeString(String str);
  public String readString();
}