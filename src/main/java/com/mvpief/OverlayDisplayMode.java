package com.mvpief;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OverlayDisplayMode
{
    ALWAYS("Always Show"),
    ON_GOAL("Show when goal is not completed"),
    ON_RUNNING("Only show when running rooftops"),
    NEVER("Never show the overlay");

    private final String name;

    @Override
    public String toString()
    {
        return name;
    }
}