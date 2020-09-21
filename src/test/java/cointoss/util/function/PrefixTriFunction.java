/*
 * Copyright (C) 2020 cointoss Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package cointoss.util.function;

import javax.annotation.processing.Generated;

@Generated("SpecializedCodeGenerator")
public interface PrefixTriFunction<Specializable> {

    /**
     * Composes a single value from three arguments.
     * 
     * @param param1 First parameter.
     * @param param2 Second parameter.
     * @param param3 Third parameter.
     * @return A calculated result.
     */
    Specializable applyAsPrefix(Specializable param1, Specializable param2, Specializable param3);
}