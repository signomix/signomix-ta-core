package com.signomix.core.application.port.out;

//import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.signomix.common.db.IotDatabaseIface;
import com.signomix.core.adapter.out.MessageService;

import io.quarkus.test.Mock;

public class DeviceCheckerTest {

    @Mock
    private IotDatabaseIface iotDao;

    @Mock
    private MessageService messageService;

    //@InjectMocks
    //private DeviceChecker deviceChecker;

    @BeforeEach
    public void setUp() {
        //MockitoAnnotations.openMocks(this);
        //deviceChecker = new DeviceChecker(iotDao, messageService);
    }

    @Test
    public void testRun() {
        // Add your test logic here
        // For example, you can verify interactions with mocks
        
        //deviceChecker.run();
       // verify(iotDao, times(1)).someMethod(); // Replace someMethod with an actual method from IotDatabaseIface
        //verify(messageService, times(1)).someMethod(); // Replace someMethod with an actual method from MessageService
    }

    @Test
    public void testRunWithPaidDevices() {
        //deviceChecker = new DeviceChecker(iotDao, messageService, true);
        //deviceChecker.run();
        // Add assertions or verifications specific to paidDevices = true
    }
}
