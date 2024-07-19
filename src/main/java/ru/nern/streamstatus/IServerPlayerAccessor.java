package ru.nern.streamstatus;

public interface IServerPlayerAccessor {
    boolean streamstatus$isStreaming();
    void streamstatus$setStreaming(boolean flag);
}
