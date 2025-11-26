<?php
require 'db.php';
$data = json_decode(file_get_contents('php://input'), true);

if (!$data || !isset($data['usuario']) || !isset($data['contrasena']) || !isset($data['nombre_completo'])) {
    http_response_code(400);
    echo json_encode(["success" => false, "msg" => "Datos incompletos"]);
    exit;
}

$usuario = $data['usuario'];
$contrasena = password_hash($data['contrasena'], PASSWORD_DEFAULT);
$nombre = $data['nombre_completo'];
$correo = $data['correo'] ?? '';

try {
    $stmt = $pdo->prepare("INSERT INTO usuarios (usuario, contrasena, nombre_completo, correo) VALUES (:usuario, :contrasena, :nombre, :correo)");
    $stmt->execute([
        'usuario' => $usuario,
        'contrasena' => $contrasena,
        'nombre' => $nombre,
        'correo' => $correo
    ]);
    echo json_encode(["success" => true, "id" => $pdo->lastInsertId()]);
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(["success" => false, "msg" => $e->getMessage()]);
}
?>

