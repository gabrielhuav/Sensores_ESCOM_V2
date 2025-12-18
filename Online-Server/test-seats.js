// test-seats.js
// Script para probar el sistema de asientos

const BASE_URL = 'http://localhost:3000';

async function testSeatSystem() {
    console.log('🧪 Iniciando pruebas del sistema de asientos...\n');

    try {
        // Test 1: Ver asientos ocupados (inicialmente vacío)
        console.log('📋 Test 1: Ver asientos ocupados inicialmente');
        const initialSeats = await fetch(`${BASE_URL}/seats/escom_salon_2001`);
        const initialData = await initialSeats.json();
        console.log('Respuesta:', JSON.stringify(initialData, null, 2));
        console.log(`✅ Asientos ocupados: ${initialData.count}\n`);

        // Test 2: Sentar al jugador 1
        console.log('🪑 Test 2: Sentar al jugador 1 en pupitre (4, 15)');
        const sitPlayer1 = await fetch(`${BASE_URL}/seats/sit`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                playerId: 'player_001',
                playerName: 'Juan Pérez García',
                map: 'escom_salon_2001',
                x: 4,
                y: 15
            })
        });
        const sitData1 = await sitPlayer1.json();
        console.log('Respuesta:', JSON.stringify(sitData1, null, 2));
        console.log(sitData1.success ? '✅ Éxito' : '❌ Falló', '\n');

        // Test 3: Sentar al jugador 2 en otro pupitre
        console.log('🪑 Test 3: Sentar al jugador 2 en pupitre (8, 15)');
        const sitPlayer2 = await fetch(`${BASE_URL}/seats/sit`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                playerId: 'player_002',
                playerName: 'María López Sánchez',
                map: 'escom_salon_2001',
                x: 8,
                y: 15
            })
        });
        const sitData2 = await sitPlayer2.json();
        console.log('Respuesta:', JSON.stringify(sitData2, null, 2));
        console.log(sitData2.success ? '✅ Éxito' : '❌ Falló', '\n');

        // Test 4: Intentar sentar al jugador 3 en el mismo pupitre que jugador 1
        console.log('❌ Test 4: Intentar ocupar pupitre ya ocupado (4, 15)');
        const sitPlayer3Fail = await fetch(`${BASE_URL}/seats/sit`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                playerId: 'player_003',
                playerName: 'Carlos Rodríguez',
                map: 'escom_salon_2001',
                x: 4,
                y: 15
            })
        });
        const sitData3Fail = await sitPlayer3Fail.json();
        console.log('Respuesta:', JSON.stringify(sitData3Fail, null, 2));
        console.log(sitData3Fail.success ? '❌ No debería tener éxito' : '✅ Correctamente rechazado', '\n');

        // Test 5: Ver asientos ocupados ahora
        console.log('📋 Test 5: Ver asientos ocupados después de sentar 2 jugadores');
        const currentSeats = await fetch(`${BASE_URL}/seats/escom_salon_2001`);
        const currentData = await currentSeats.json();
        console.log('Respuesta:', JSON.stringify(currentData, null, 2));
        console.log(`✅ Total de asientos ocupados: ${currentData.count}\n`);

        // Test 6: Intentar que jugador 1 se siente en otro pupitre sin levantarse primero
        console.log('❌ Test 6: Intentar sentarse en otro pupitre sin levantarse');
        const sitPlayer1Again = await fetch(`${BASE_URL}/seats/sit`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                playerId: 'player_001',
                playerName: 'Juan Pérez García',
                map: 'escom_salon_2001',
                x: 12,
                y: 15
            })
        });
        const sitData1Again = await sitPlayer1Again.json();
        console.log('Respuesta:', JSON.stringify(sitData1Again, null, 2));
        console.log(sitData1Again.success ? '❌ No debería tener éxito' : '✅ Correctamente rechazado', '\n');

        // Test 7: Levantar al jugador 1
        console.log('🚶 Test 7: Levantar al jugador 1');
        const standPlayer1 = await fetch(`${BASE_URL}/seats/stand`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                playerId: 'player_001',
                map: 'escom_salon_2001'
            })
        });
        const standData1 = await standPlayer1.json();
        console.log('Respuesta:', JSON.stringify(standData1, null, 2));
        console.log(standData1.success ? '✅ Éxito' : '❌ Falló', '\n');

        // Test 8: Ahora jugador 1 puede sentarse en otro pupitre
        console.log('🪑 Test 8: Jugador 1 se sienta en nuevo pupitre (12, 15)');
        const sitPlayer1New = await fetch(`${BASE_URL}/seats/sit`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                playerId: 'player_001',
                playerName: 'Juan Pérez García',
                map: 'escom_salon_2001',
                x: 12,
                y: 15
            })
        });
        const sitData1New = await sitPlayer1New.json();
        console.log('Respuesta:', JSON.stringify(sitData1New, null, 2));
        console.log(sitData1New.success ? '✅ Éxito' : '❌ Falló', '\n');

        // Test 9: Ver estado final de asientos
        console.log('📋 Test 9: Estado final de asientos');
        const finalSeats = await fetch(`${BASE_URL}/seats/escom_salon_2001`);
        const finalData = await finalSeats.json();
        console.log('Respuesta:', JSON.stringify(finalData, null, 2));
        console.log(`✅ Total de asientos ocupados: ${finalData.count}`);
        
        console.log('\n📊 Resumen de asientos ocupados:');
        finalData.seats.forEach((seat, index) => {
            console.log(`  ${index + 1}. ${seat.playerName} en pupitre (${seat.x}, ${seat.y})`);
        });

        // Test 10: Limpiar - Levantar a todos los jugadores
        console.log('\n🧹 Test 10: Limpiar - Levantar a todos los jugadores');
        
        await fetch(`${BASE_URL}/seats/stand`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                playerId: 'player_001',
                map: 'escom_salon_2001'
            })
        });
        
        await fetch(`${BASE_URL}/seats/stand`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                playerId: 'player_002',
                map: 'escom_salon_2001'
            })
        });
        
        const cleanSeats = await fetch(`${BASE_URL}/seats/escom_salon_2001`);
        const cleanData = await cleanSeats.json();
        console.log('✅ Asientos después de limpiar:', cleanData.count);

        console.log('\n✅ ¡Todas las pruebas completadas exitosamente!');

    } catch (error) {
        console.error('\n❌ Error durante las pruebas:', error.message);
    }
}

// Ejecutar las pruebas
console.log('═══════════════════════════════════════════════════════');
console.log('  PRUEBAS DEL SISTEMA DE ASIENTOS (PUPITRES)');
console.log('═══════════════════════════════════════════════════════\n');

testSeatSystem()
    .then(() => {
        console.log('\n═══════════════════════════════════════════════════════');
        console.log('  FIN DE LAS PRUEBAS');
        console.log('═══════════════════════════════════════════════════════');
    })
    .catch(error => {
        console.error('Error fatal:', error);
    });
