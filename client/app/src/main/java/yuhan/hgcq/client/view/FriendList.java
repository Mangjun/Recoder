package yuhan.hgcq.client.view;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import yuhan.hgcq.client.R;
import yuhan.hgcq.client.adapter.FollowerAdapter;
import yuhan.hgcq.client.adapter.FollowingAdapter;
import yuhan.hgcq.client.controller.FollowController;
import yuhan.hgcq.client.model.dto.follow.FollowDTO;
import yuhan.hgcq.client.model.dto.follow.Follower;
import yuhan.hgcq.client.model.dto.member.MemberDTO;
import yuhan.hgcq.client.model.dto.team.TeamDTO;

public class FriendList extends AppCompatActivity {
    /* View */
    TextView empty;
    EditText searchText;
    ImageButton friendAdd;
    Button follower, following;
    RecyclerView friendListView;
    BottomNavigationView navi;

    /* Adapter */
    FollowerAdapter fra;
    FollowingAdapter fga;

    /* http 통신 */
    FollowController fc;

    /* 받아올 값 */
    boolean isPrivate;
    MemberDTO loginMember;

    /* 팔로워, 팔로우 버튼 구별 */
    boolean isFollower = false;

    /* 메인 스레드 */
    Handler handler = new Handler(Looper.getMainLooper());
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: // 뒤로가기 버튼 ID
                finish(); // 현재 액티비티 종료
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar(); // actionBar 가져오기
        if (actionBar != null) {
            actionBar.setDisplayShowCustomEnabled(true); // 커스텀 뷰 사용 허용
            actionBar.setDisplayShowTitleEnabled(false); // 기본 제목 비활성화
            actionBar.setDisplayHomeAsUpEnabled(true);
            // 액션바 배경 색상 설정
            actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#c2dcff")));

            // 커스텀 타이틀 텍스트뷰 설정
            TextView customTitle = new TextView(this);
            customTitle.setText("친구 목록"); // 제목 텍스트 설정
            customTitle.setTextSize(20); // 텍스트 크기 조정
            customTitle.setTypeface(ResourcesCompat.getFont(this, R.font.hangle_l)); // 폰트 설정
            customTitle.setTextColor(getResources().getColor(R.color.white)); // 텍스트 색상 설정

            actionBar.setCustomView(customTitle); // 커스텀 뷰 설정
        }


        EdgeToEdge.enable(this);
        /* Layout */
        setContentView(R.layout.activity_friend_list);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        /* 초기화 */
        fc = new FollowController(this);

        empty = findViewById(R.id.empty);
        searchText = findViewById(R.id.searchText);
        friendAdd = findViewById(R.id.friendAdd);
        follower = findViewById(R.id.follower);
        following = findViewById(R.id.following);
        friendListView = findViewById(R.id.friendList);
        navi = findViewById(R.id.bottom_navigation_view);

        /* 관련된 페이지 */
        Intent groupMainPage = new Intent(this, GroupMain.class);
        Intent albumMainPage = new Intent(this, AlbumMain.class);
        Intent friendListPage = new Intent(this, FriendList.class);
        Intent likePage = new Intent(this, Like.class);
        Intent myPage = new Intent(this, MyPage.class);
        Intent friendAddPage = new Intent(this, FriendAdd.class);

        /* 받아 올 값 */
        Intent getIntent = getIntent();
        isPrivate = getIntent.getBooleanExtra("isPrivate", false);
        loginMember = (MemberDTO) getIntent.getSerializableExtra("loginMember");

        /* 초기 설정 */
        fc.followingList(new Callback<List<MemberDTO>>() {

            @Override
            public void onResponse(Call<List<MemberDTO>> call, Response<List<MemberDTO>> response) {
                if (response.isSuccessful()) {
                    searchText.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                        }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                            String text=s.toString();
                            fc.searchFollowingByName("%" + text + "%", new Callback<List<MemberDTO>>() {
                                @Override
                                public void onResponse(Call<List<MemberDTO>> call, Response<List<MemberDTO>> response) {
                                    if (response.isSuccessful()){
                                        List<MemberDTO> following=response.body();
                                        if(following!=null){
                                            handler.post(()->{
                                                if(following.isEmpty()){
                                                    empty.setVisibility(View.VISIBLE);
                                                }else{
                                                    empty.setVisibility(View.INVISIBLE);
                                                }
                                            });
                                            handler.post(() -> {
                                                fga.updateList(following);
                                            });

                                        }else{
                                            Log.i("Found Private Follower By Name", "Fail");
                                        }

                                    }else{
                                        Log.e("Search Error", "Failed to fetch Follower: " + response.message());
                                    }
                                }

                                @Override
                                public void onFailure(Call<List<MemberDTO>> call, Throwable t) {
                                    Log.e("Search Error", "Request failed: " + t.getMessage());
                                    handler.post(() -> {
                                        Toast.makeText(FriendList.this, "팔로잉 검색에 실패했습니다.", Toast.LENGTH_SHORT).show();
                                    });
                                }
                            });
                        }

                        @Override
                        public void afterTextChanged(Editable s) {

                        }
                    });
                    List<MemberDTO> followingList = response.body();
                    if (followingList.isEmpty()) {
                        handler.post(() -> {
                            empty.setVisibility(View.VISIBLE);
                        });
                    } else {
                        handler.post(() -> {
                            empty.setVisibility(View.INVISIBLE);
                        });
                    }
                    fga = new FollowingAdapter(FriendList.this, followingList);
                    handler.post(() -> {
                        friendListView.setAdapter(fga);
                    });
                } else {
                    /* Toast 메시지 */
                }
            }

            @Override
            public void onFailure(Call<List<MemberDTO>> call, Throwable t) {
                /* Toast 메시지 */
            }
        });

        /* 팔로워 */
        follower.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchText.addTextChangedListener(new TextWatcher() {

                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        String text=s.toString();

                        fc.searchFollowerByName("%" + text + "%", new Callback<Follower>() {
                            @Override
                            public void onResponse(Call<Follower> call, Response<Follower> response) {
                                if (response.isSuccessful()){
                                    List<MemberDTO> followers=response.body().getFollowerList();
                                    if(followers!=null){
                                        if(followers.isEmpty()){
                                            handler.post(()->{
                                               empty.setVisibility(View.VISIBLE);
                                            });
                                        }else{
                                            handler.post(()->{
                                               empty.setVisibility(View.INVISIBLE);
                                            });
                                        }
                                        handler.post(() -> {
                                            fra.updateList(followers);
                                        });
                                    }else{
                                        Log.i("Found Private Follower By Name", "Fail");
                                    }

                                }else{
                                    Log.e("Search Error", "Failed to fetch Follower: " + response.message());
                                }
                            }

                            @Override
                            public void onFailure(Call<Follower> call, Throwable t) {
                                Log.e("Search Error", "Request failed: " + t.getMessage());
                                handler.post(() -> {
                                    Toast.makeText(FriendList.this, "팔로워 검색에 실패했습니다.", Toast.LENGTH_SHORT).show();
                                });
                            }
                        });
                    }

                    @Override
                    public void afterTextChanged(Editable s) {

                    }
                });
                fc.followerList(new Callback<Follower>() {
                    @Override
                    public void onResponse(Call<Follower> call, Response<Follower> response) {
                        if (response.isSuccessful()) {
                            Follower body = response.body();

                            List<MemberDTO> followerList = body.getFollowerList();
                            List<MemberDTO> followingList = body.getFollowingList();

                            if (followerList.isEmpty()) {
                                handler.post(() -> {
                                    empty.setVisibility(View.VISIBLE);
                                });
                            } else {
                                handler.post(() -> {
                                    empty.setVisibility(View.INVISIBLE);
                                });
                            }
                            fra = new FollowerAdapter(FriendList.this, followerList, followingList);
                            handler.post(() -> {
                                friendListView.setAdapter(fra);
                            });
                            isFollower = true;
                        } else {
                            /* Toast 메시지 */
                        }
                    }

                    @Override
                    public void onFailure(Call<Follower> call, Throwable t) {
                        /* Toast 메시지 */
                    }
                });
            }

        });


        /* 팔로잉 */
        following.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                fc.followingList(new Callback<List<MemberDTO>>() {
                    @Override
                    public void onResponse(Call<List<MemberDTO>> call, Response<List<MemberDTO>> response) {
                        if (response.isSuccessful()) {
                            List<MemberDTO> followingList = response.body();
                            if (followingList.isEmpty()) {
                                handler.post(() -> {
                                    empty.setVisibility(View.VISIBLE);
                                });
                            } else {
                                handler.post(() -> {
                                    empty.setVisibility(View.INVISIBLE);
                                });
                            }
                            fga = new FollowingAdapter(FriendList.this, followingList);
                            handler.post(() -> {
                                friendListView.setAdapter(fga);
                            });
                            isFollower = false;
                        } else {
                            /* Toast 메시지 */
                        }
                    }

                    @Override
                    public void onFailure(Call<List<MemberDTO>> call, Throwable t) {
                        /* Toast 메시지 */
                    }
                });
            }
        });

        /* 팔로우 추가 */
        friendAdd.setOnClickListener(v -> {
            if (isPrivate) {
                friendAddPage.putExtra("isPrivate", isPrivate);
            }
            friendAddPage.putExtra("loginMember", loginMember);
            startActivity(friendAddPage);
        });

        /* 네비게이션 */
        navi.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                int itemId = menuItem.getItemId();
                if (itemId == R.id.fragment_home) {
                    if (isPrivate) {
                        albumMainPage.putExtra("isPrivate", true);
                        albumMainPage.putExtra("loginMember", loginMember);
                        startActivity(albumMainPage);
                    } else {
                        groupMainPage.putExtra("loginMember", loginMember);
                        startActivity(groupMainPage);
                    }
                    return true;
                } else if (itemId == R.id.fragment_friend) {
                    if (loginMember == null) {
                        Toast.makeText(FriendList.this, "로그인 후 이용 가능합니다.", Toast.LENGTH_SHORT).show();
                    } else {
                        if (isPrivate) {
                            friendListPage.putExtra("isPrivate", true);
                        }
                        friendListPage.putExtra("loginMember", loginMember);
                        startActivity(friendListPage);
                    }
                    return true;
                } else if (itemId == R.id.fragment_like) {
                    if (isPrivate) {
                        likePage.putExtra("isPrivate", true);
                    }
                    likePage.putExtra("loginMember", loginMember);
                    startActivity(likePage);
                    return true;
                } else if (itemId == R.id.fragment_setting) {
                    if (loginMember == null) {
                        Toast.makeText(FriendList.this, "로그인 후 이용 가능합니다.", Toast.LENGTH_SHORT).show();
                    } else {
                        if (isPrivate) {
                            myPage.putExtra("isPrivate", true);
                        }
                        myPage.putExtra("loginMember", loginMember);
                        startActivity(myPage);
                    }
                    return true;
                }
                return false;
            }
        });
    }

    /* Confirm 창 */
    public void onClick_setting_costume_save(String message,
                                             DialogInterface.OnClickListener positive,
                                             DialogInterface.OnClickListener negative) {

        new AlertDialog.Builder(this)
                .setTitle("Recoder")
                .setMessage(message)
                .setIcon(R.drawable.album)
                .setPositiveButton(android.R.string.yes, positive)
                .setNegativeButton(android.R.string.no, negative)
                .show();
    }

    /* 화면 이벤트 처리 */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) ev.getRawX(), (int) ev.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }
}