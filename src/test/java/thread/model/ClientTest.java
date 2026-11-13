package thread.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import thread.model.enums.Rate;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class ClientTest {
    @Test
    @DisplayName("валидные данные.")
    public void createValidClient(){
        long id = 1;
        String name = "M M";
        Rate rate = Rate.BASIC;
        LocalDate date = LocalDate.of(2019,2,10);
        ZoneId zoneId = ZoneId.systemDefault();

        Client client = new Client(id,name,rate,date,zoneId) ;

        assertEquals(id,client.id());
    }


    @ParameterizedTest
    @ValueSource(longs = {-1,0})
    @DisplayName("Тестирование негативных сценариев id")
    public void shouldExceptionWithNegativeId(long brokenId){
        String name = "M M";
        Rate rate = Rate.BASIC;
        LocalDate date = LocalDate.of(2019,2,10);
        ZoneId zoneId = ZoneId.systemDefault();

        assertThrows(IllegalArgumentException.class,()-> new Client(brokenId,name,rate,date,zoneId));
    }


    @ParameterizedTest
    @ValueSource(strings = {" ",""})
    public void shouldThrowWithNameIsNullOrIsBlank(String name){
        Rate rate = Rate.BASIC;
        LocalDate date = LocalDate.of(2019,2,10);
        ZoneId zoneId = ZoneId.systemDefault();

        assertThrows(IllegalArgumentException.class,()->new Client(1,name,rate,date,zoneId));
        assertThrows(IllegalArgumentException.class,()->new Client(1,null,rate,date,zoneId));
    }

    @Test
    public void notValidRandomParam(){

        assertThrows(IllegalArgumentException.class,()->new Client(1,"name",Rate.PREMIUM,LocalDate.now(),null));
        assertThrows(IllegalArgumentException.class,()-> new Client(1,"name",Rate.BASIC,null,ZoneId.systemDefault()));
        assertThrows(IllegalArgumentException.class,()->new Client(1,"name",null,LocalDate.now(),ZoneId.systemDefault()));
    }
}
