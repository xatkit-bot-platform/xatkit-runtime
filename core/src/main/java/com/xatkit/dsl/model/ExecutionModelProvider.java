package com.xatkit.dsl.model;

import com.xatkit.execution.ExecutionModel;
import lombok.NonNull;

public interface ExecutionModelProvider {

    @NonNull ExecutionModel getExecutionModel();
}
