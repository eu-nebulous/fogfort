package eu.nebulouscloud.fogfort.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import eu.nebulouscloud.fogfort.model.jobs.Job;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class JobSchedulerService {
	Map<String, Job> jobs = new HashMap();
}
