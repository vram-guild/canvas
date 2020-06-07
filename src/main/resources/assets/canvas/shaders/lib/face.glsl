#define  FACE_DOWN  0
#define  FACE_UP    1
#define  FACE_NORTH 2
#define  FACE_SOUTH 3
#define  FACE_WEST  4
#define  FACE_EAST  5

const mat3[6] UV_MATRIX = mat3[6](
        mat3(1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0),
        mat3(1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0),
        mat3(1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0),
        mat3(1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0),
        mat3(0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0),
        mat3(0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0)
);

int face(vec3 normal) {
    vec3 a = abs(normal);
    float m = max(max(a.x, a.y), a.z);

    return a.x == m ? (normal.x > 0 ? FACE_EAST : FACE_WEST)
            : a.y == m ? (normal.y > 0 ? FACE_UP : FACE_DOWN)
                    : (normal.z > 0 ? FACE_SOUTH : FACE_NORTH);
}


// TODO: rename to faceUv
vec2 uv(vec3 pos, vec3 normal) {
    mat3 m = UV_MATRIX[face(normal)];
    vec3 result = m * pos;
    return result.xy;
}
