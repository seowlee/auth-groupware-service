package pharos.groupware.service.common.excel;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import pharos.groupware.service.domain.leave.entity.LeaveBalance;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
public class LeaveBalanceExcelExporter {

    public Path export(List<LeaveBalance> balances, Map<Long, String> userNameMap, Path exportDir) {
        try {
            if (!Files.exists(exportDir)) {
                Files.createDirectories(exportDir);
            }
            String filename = "leave_balances_" + LocalDate.now() + ".xlsx";
            Path file = exportDir.resolve(filename);

            try (Workbook wb = new XSSFWorkbook()) {
                Sheet sheet = wb.createSheet("Balances");

                // Header
                Row header = sheet.createRow(0);
                header.createCell(0).setCellValue("User");
                header.createCell(1).setCellValue("Leave Type");
                header.createCell(2).setCellValue("Year Number");
                header.createCell(3).setCellValue("Total Allocated");
                header.createCell(4).setCellValue("Used");

                int rowIdx = 1;
                for (LeaveBalance lb : balances) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(userNameMap.get(lb.getUserId()));
                    row.createCell(1).setCellValue(lb.getLeaveType().name());
                    row.createCell(2).setCellValue(lb.getYearNumber());
                    row.createCell(3).setCellValue(lb.getTotalAllocated().doubleValue());
                    row.createCell(4).setCellValue(lb.getUsed().doubleValue());
                }

                try (FileOutputStream fos = new FileOutputStream(file.toFile())) {
                    wb.write(fos);
                }
            }
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException("엑셀 파일 생성 실패", e);
        }
    }
}

