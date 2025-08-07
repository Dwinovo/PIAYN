package com.dwinovo.piayn.server.resource.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServerModelData {
    @NonNull
    private String modelName;
    @NonNull
    private String modelID;
    @NonNull
    private byte[] model;
    @NonNull
    private byte[] animation;
    @NonNull
    private byte[] texture;
}
