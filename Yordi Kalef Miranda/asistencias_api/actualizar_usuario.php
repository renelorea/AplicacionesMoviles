<?php
require 'db.php';
$data = json_decode(file_get_contents('php://input'), true);

if (!$data || !isset($data['id']) || !isset($data['usuario']) || !isset($data['nombre_completo'])) {
    http_response_code(400);
    echo json_encode(["success" => false, "msg" => "Datos incompletos"]);
    exit;
}

$id = $data['id'];
$usuario = $data['usuario'];
$nombre = $data['nombre_completo'];
$correo = $data['correo'] ?? '';
$contrasena = $data['contrasena'] ?? null;

try {
    if ($contrasena) {
        $hash = password_hash($contrasena, PASSWORD_DEFAULT);
        $stmt = $pdo->prepare("UPDATE usuarios SET usuario = :usuario, nombre_completo = :nombre, correo = :correo, contrasena = :contrasena WHERE id = :id");
        $stmt->execute([
            'usuario' => $usuario,
            'nombre' => $nombre,
            'correo' => $correo,
            'contrasena' => $hash,
            'id' => $id
        ]);
    } else {
        $stmt = $pdo->prepare("UPDATE usuarios SET usuario = :usuario, nombre_completo = :nombre, correo = :correo WHERE id = :id");
        $stmt->execute([
            'usuario' => $usuario,
            'nombre' => $nombre,
            'correo' => $correo,
            'id' => $id
        ]);
    }
    echo json_encode(["success" => true]);
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(["success" => false, "msg" => $e->getMessage()]);
}
?>
