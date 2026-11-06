package com.hotel.entity;

import java.math.BigDecimal;

public record Room(long id, String roomNumber, String roomType,
                   BigDecimal dailyPrice, String status) {}
