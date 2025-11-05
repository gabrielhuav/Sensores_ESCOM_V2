// fix-existing-dates.js
// Script para corregir fechas existentes con desfase de 6 horas

const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient();

async function fixExistingDates() {
    try {
        console.log('🔧 Iniciando corrección de fechas existentes...\n');
        
        // Obtener todos los registros
        const records = await prisma.attendance.findMany({
            orderBy: { id: 'asc' }
        });
        
        if (records.length === 0) {
            console.log('✅ No hay registros para corregir.');
            return;
        }
        
        console.log(`📊 Se encontraron ${records.length} registros para revisar.\n`);
        
        let correctedCount = 0;
        
        for (const record of records) {
            const originalDate = new Date(record.attendanceTime);
            
            // Restar 6 horas (UTC-6) - 21600000 milisegundos
            const correctedDate = new Date(originalDate.getTime() - (6 * 60 * 60 * 1000));
            
            // Actualizar el registro
            await prisma.attendance.update({
                where: { id: record.id },
                data: { attendanceTime: correctedDate }
            });
            
            correctedCount++;
            
            console.log(`✅ ID ${record.id} - ${record.fullName}`);
            console.log(`   Antes: ${originalDate.toISOString()}`);
            console.log(`   Después: ${correctedDate.toISOString()}`);
            console.log(`   México: ${correctedDate.toLocaleString('es-MX', { timeZone: 'America/Mexico_City' })}\n`);
        }
        
        console.log(`\n🎉 ¡Corrección completada!`);
        console.log(`📝 Total de registros corregidos: ${correctedCount}`);
        
    } catch (error) {
        console.error('❌ Error al corregir fechas:', error);
        throw error;
    } finally {
        await prisma.$disconnect();
    }
}

// Ejecutar el script
console.log('=' .repeat(60));
console.log('SCRIPT DE CORRECCIÓN DE ZONA HORARIA');
console.log('Restando 6 horas a todas las fechas existentes');
console.log('=' .repeat(60));
console.log('\n⚠️  ADVERTENCIA: Este script modificará todos los registros');
console.log('   de la tabla Attendance. Asegúrate de tener un respaldo.\n');

// Dar tiempo para cancelar (Ctrl+C)
setTimeout(() => {
    fixExistingDates()
        .then(() => {
            console.log('\n✅ Script finalizado exitosamente.');
            process.exit(0);
        })
        .catch((error) => {
            console.error('\n❌ El script terminó con errores:', error);
            process.exit(1);
        });
}, 3000);

console.log('⏳ Iniciando en 3 segundos... (Presiona Ctrl+C para cancelar)');
