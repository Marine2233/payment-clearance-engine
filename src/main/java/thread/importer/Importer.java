package thread.importer;

import thread.model.Client;
import thread.model.PaymentRequest;
import thread.model.enums.PaymentCategory;
import thread.model.enums.Rate;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class Importer {

    public ImportResult<PaymentRequest> readCsvPayment(Path path){
        List<ErrorResult> error = new ArrayList<>();
        List<PaymentRequest> payments = new ArrayList<>();
        int numLine = 1;
        String line ="";

        try(BufferedReader reader = Files.newBufferedReader(path,StandardCharsets.UTF_8)){
           reader.readLine();

           while ((line = reader.readLine()) != null){
               numLine++;

               if (line.isBlank()){
                   continue;
               }

               try{

                   String[]parts = line.split(";",-1);

                   if (parts.length != 6){
                       throw new IllegalArgumentException("Ошибка битой строки" + line);
                   }

                   BigInteger id = new BigInteger(parts[0].trim());
                   long cliId = Long.parseLong(parts[1].trim());
                   BigDecimal amount= new BigDecimal(parts[2].trim());
                   PaymentCategory category = PaymentCategory.valueOf(parts[3].trim());
                   Instant createAt = Instant.parse(parts[4].trim());
                   LocalDateTime scheduledPayment = LocalDateTime.parse(parts[5].trim());

                   PaymentRequest request = new PaymentRequest(id,cliId,amount,category,createAt,scheduledPayment);
                   payments.add(request);

               }catch (Exception e){
                   String errorMsg = e.getMessage() == null? e.getClass().getSimpleName() : e.getMessage();
                   error.add(new ErrorResult(numLine,line,errorMsg));
               }
           }
        }catch (IOException e){
            throw new RuntimeException("Ошибка чтения файла платежей: " + path, e);
        }
        return new ImportResult<>(payments,error);
    }

    public ImportResult<Client> readCsvClient(Path path){
        List<ErrorResult> errors = new ArrayList<>();
        List<Client> clients = new ArrayList<>();
        Set<Long> ids = new HashSet<>();
        int numLine  = 1;
        String line = "";

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)){
            reader.readLine();

            while ((line = reader.readLine())!= null){
                numLine++;

                if (line.isBlank()){
                    continue;
                }

                try {
                    String[] parts = line.split(";", -1);

                    if (parts.length != 5) {
                        throw new IllegalArgumentException("битая строка клиента.");
                    }

                    long id = Long.parseLong(parts[0].trim());
                    String name = parts[1].trim();
                    Rate rate = Rate.valueOf(parts[2].trim());
                    ZoneId zoneId = ZoneId.of(parts[3].trim());
                    LocalDate registry = LocalDate.parse(parts[4].trim());

                    Client client = new Client(id, name, rate, registry, zoneId);


                    if (!ids.add(id)) {
                        throw new IllegalArgumentException("Дубликат клиента: " + id);
                    }

                    clients.add(client);

                }catch (Exception e){
                    String error = e.getMessage() == null? e.getClass().getSimpleName() : e.getMessage();
                    errors.add(new ErrorResult(numLine,line,error));
                }

            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return  new ImportResult<>(List.copyOf(clients),errors);
    }

}
