package com.aaronbrecher.neverlate.database;

import javax.inject.Inject;

public class EventCompatabilityRepository {
    EventCompatabilityDao mDao;

    @Inject
    public EventCompatabilityRepository(EventCompatabilityDao dao){
        this.mDao = dao;
    }
}
