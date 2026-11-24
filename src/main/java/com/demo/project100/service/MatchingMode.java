package com.demo.project100.service;

/** Controls whether an incoming order must fill completely or may fill in parts. */
public enum MatchingMode {
    WHOLE_ORDER,
    PARTIAL
}
