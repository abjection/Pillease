<?php
header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST, GET, DELETE");

$host = "localhost";
$username = "root";
$password = "";
$database = "pillease_db";

$conn = new mysqli($host, $username, $password, $database);

if ($conn->connect_error) {
    echo json_encode(["success" => false, "message" => "Database connection failed"]);
    exit();
}

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    $action = $_POST['action'] ?? 'create';
    
    if ($action == 'delete') {
        $history_id = $_POST['history_id'] ?? 0;
        $stmt = $conn->prepare("DELETE FROM medical_history WHERE history_id = ?");
        $stmt->bind_param("i", $history_id);
        
        if ($stmt->execute()) {
            echo json_encode(["success" => true, "message" => "Record deleted"]);
        } else {
            echo json_encode(["success" => false, "message" => "Failed to delete"]);
        }
        $stmt->close();
    } else {
        $user_id = $_POST['user_id'] ?? null;
        $record_name = $_POST['record_name'] ?? '';
        $record_date = $_POST['record_date'] ?? '';

        if (empty($record_name) || empty($record_date)) {
            echo json_encode(["success" => false, "message" => "Required fields missing"]);
            exit();
        }

        $stmt = $conn->prepare("INSERT INTO medical_history (user_id, record_name, record_date) VALUES (?, ?, ?)");
        $stmt->bind_param("iss", $user_id, $record_name, $record_date);

        if ($stmt->execute()) {
            echo json_encode(["success" => true, "message" => "Medical record added!", "history_id" => $stmt->insert_id]);
        } else {
            echo json_encode(["success" => false, "message" => "Failed to save record"]);
        }
        $stmt->close();
    }
}

else if ($_SERVER['REQUEST_METHOD'] == 'GET') {
    $user_id = $_GET['user_id'] ?? null;
    
    if ($user_id) {
        $stmt = $conn->prepare("SELECT * FROM medical_history WHERE user_id = ? ORDER BY created_at DESC");
        $stmt->bind_param("i", $user_id);
        $stmt->execute();
        $result = $stmt->get_result();
        
        $records = [];
        while ($row = $result->fetch_assoc()) {
            $records[] = $row;
        }
        echo json_encode(["success" => true, "data" => $records]);
        $stmt->close();
    } else {
        echo json_encode(["success" => false, "message" => "User ID required"]);
    }
}

$conn->close();
?>
